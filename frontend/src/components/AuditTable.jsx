import React, { useState } from 'react';
import ManualLinkModal from './ManualLinkModal';
import { AlertTriangle, CheckCircle, Clock, XCircle, Filter, ArrowUpDown, Trash2 } from 'lucide-react';

const StatusBadge = ({ status }) => {
    switch (status) {
        case 'HEALED':
            return (
                <span className="px-3 py-1 rounded-full text-xs font-bold bg-green-100 text-green-700 flex items-center gap-1 w-fit">
                    <CheckCircle size={14} /> HEALED
                </span>
            );
        case 'ORPHANED':
            return (
                <span className="px-3 py-1 rounded-full text-xs font-bold bg-amber-100 text-amber-700 flex items-center gap-1 w-fit animate-pulse">
                    <XCircle size={14} /> ORPHANED
                </span>
            );
        case 'HEALING':
            return (
                <span className="px-3 py-1 rounded-full text-xs font-bold bg-blue-100 text-blue-700 flex items-center gap-1 w-fit">
                    <Clock size={14} className="animate-spin" /> HEALING
                </span>
            );
        case 'ERROR':
            return (
                <span className="px-3 py-1 rounded-full text-xs font-bold bg-red-100 text-red-700 flex items-center gap-1 w-fit">
                    <AlertTriangle size={14} /> CONNECTION FAILED
                </span>
            );
        default:
            return (
                <span className="px-3 py-1 rounded-full text-xs font-bold bg-slate-100 text-slate-500">
                    {status || 'UNKNOWN'}
                </span>
            );
    }
};

export default function AuditTable({ recordings, onRefresh, onSelectionChange }) {
    const [modalOpen, setModalOpen] = useState(false);
    const [selectedRecording, setSelectedRecording] = useState(null);
    const [selectedIds, setSelectedIds] = useState([]);
    const [filterStatus, setFilterStatus] = useState('ALL');
    const [sortConfig, setSortConfig] = useState({ key: 'updatedAt', direction: 'desc' });

    const toggleSelection = (id) => {
        const newSelection = selectedIds.includes(id)
            ? selectedIds.filter(selectedId => selectedId !== id)
            : [...selectedIds, id];

        setSelectedIds(newSelection);
        if (onSelectionChange) onSelectionChange(newSelection);
    };

    const toggleAll = () => {
        if (selectedIds.length === recordings.length) {
            setSelectedIds([]);
            if (onSelectionChange) onSelectionChange([]);
        } else {
            const allIds = recordings.map(r => r.id);
            setSelectedIds(allIds);
            if (onSelectionChange) onSelectionChange(allIds);
        }
    };

    const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);

    const handleDelete = () => {
        if (!selectedIds.length) return;
        setDeleteConfirmOpen(true);
    };

    const confirmDelete = async () => {
        setDeleteConfirmOpen(false);
        try {
            const res = await fetch(`${import.meta.env.VITE_API_URL}/recordings`, {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(selectedIds)
            });

            if (res.ok) {
                // Clear selection and refresh
                setSelectedIds([]);
                if (onSelectionChange) onSelectionChange([]);
                if (onRefresh) onRefresh();
            } else {
                alert("Failed to delete records.");
            }
        } catch (err) {
            console.error("Delete failed", err);
            alert("Error deleting records.");
        }
    };

    const handleSort = (key) => {
        let direction = 'asc';
        if (sortConfig.key === key && sortConfig.direction === 'asc') {
            direction = 'desc';
        }
        setSortConfig({ key, direction });
    };

    const filteredRecordings = recordings ? recordings.filter(rec => {
        if (filterStatus === 'ALL') return true;
        const status = rec.status || (rec.work ? 'HEALED' : 'ORPHANED');
        return status === filterStatus;
    }) : [];

    const sortedRecordings = [...filteredRecordings].sort((a, b) => {
        const aValue = a[sortConfig.key];
        const bValue = b[sortConfig.key];

        if (!aValue && !bValue) return 0;
        if (!aValue) return 1;
        if (!bValue) return -1;

        if (aValue < bValue) {
            return sortConfig.direction === 'asc' ? -1 : 1;
        }
        if (aValue > bValue) {
            return sortConfig.direction === 'asc' ? 1 : -1;
        }
        return 0;
    });

    const handleOpenModal = (recording) => {
        setSelectedRecording(recording);
        setModalOpen(true);
    };

    const handleLinkSuccess = () => {
        if (onRefresh) {
            onRefresh();
        }
    };

    if (!recordings || recordings.length === 0) return (
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-8 text-center text-slate-500 italic">
            No recordings found in the catalog.
        </div>
    );

    return (
        <>
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden relative">
                <div className="p-6 border-b border-slate-100 flex justify-between items-center">
                    <h3 className="font-bold text-slate-800">Repertoire Audit Log</h3>

                    <div className="flex items-center gap-4">
                        <span className="text-xs text-slate-500">{selectedIds.length} selected</span>

                        {selectedIds.length > 0 && (
                            <button
                                onClick={handleDelete}
                                className="flex items-center gap-1 text-xs font-bold text-red-600 bg-red-50 hover:bg-red-100 px-3 py-1.5 rounded-lg transition"
                                title="Delete Selected"
                            >
                                <Trash2 size={14} /> Delete ({selectedIds.length})
                            </button>
                        )}

                        <div className="relative">
                            <Filter size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                            <select
                                value={filterStatus}
                                onChange={(e) => setFilterStatus(e.target.value)}
                                className="pl-9 pr-4 py-1.5 text-sm bg-slate-50 border border-slate-200 rounded-lg text-slate-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
                            >
                                <option value="ALL">All Statuses</option>
                                <option value="HEALED">Healed</option>
                                <option value="ORPHANED">Orphaned</option>
                                <option value="HEALING">Healing</option>
                                <option value="ERROR">Connection Failed</option>
                            </select>
                        </div>
                    </div>
                </div>
                <table className="w-full text-left border-collapse">
                    <thead className="bg-slate-50 border-b border-slate-200">
                        <tr>
                            <th className="p-4 w-10">
                                <input
                                    type="checkbox"
                                    checked={recordings.length > 0 && selectedIds.length === recordings.length}
                                    onChange={toggleAll}
                                    className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                                />
                            </th>
                            <th className="p-4 font-semibold text-slate-700 cursor-pointer hover:bg-slate-100" onClick={() => handleSort('isrc')}>
                                <div className="flex items-center gap-1">ISRC <ArrowUpDown size={14} className="text-slate-400" /></div>
                            </th>
                            <th className="p-4 font-semibold text-slate-700 cursor-pointer hover:bg-slate-100" onClick={() => handleSort('recordingTitle')}>
                                <div className="flex items-center gap-1">Title <ArrowUpDown size={14} className="text-slate-400" /></div>
                            </th>
                            <th className="p-4 font-semibold text-slate-700 cursor-pointer hover:bg-slate-100" onClick={() => handleSort('artistName')}>
                                <div className="flex items-center gap-1">Artist <ArrowUpDown size={14} className="text-slate-400" /></div>
                            </th>
                            <th className="p-4 font-semibold text-slate-700">ISWC (Work)</th>
                            <th className="p-4 font-semibold text-slate-700 cursor-pointer hover:bg-slate-100" onClick={() => handleSort('updatedAt')}>
                                <div className="flex items-center gap-1">Last Updated <ArrowUpDown size={14} className="text-slate-400" /></div>
                            </th>
                            <th className="p-4 font-semibold text-slate-700">Status</th>
                            <th className="p-4 font-semibold text-slate-700">Action</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                        {sortedRecordings.map((rec) => (
                            <tr key={rec.id} className={`hover:bg-slate-50 transition ${selectedIds.includes(rec.id) ? 'bg-blue-50/50' : ''}`}>
                                <td className="p-4">
                                    <input
                                        type="checkbox"
                                        checked={selectedIds.includes(rec.id)}
                                        onChange={() => toggleSelection(rec.id)}
                                        className="rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                                    />
                                </td>
                                <td className="p-4 font-mono text-sm text-blue-600">{rec.isrc || 'N/A'}</td>
                                <td className="p-4 text-slate-800 font-medium">{rec.recordingTitle}</td>
                                <td className="p-4 text-slate-600">{rec.artistName || 'Unknown'}</td>
                                <td className="p-4">
                                    {rec.work ? (
                                        <span className="font-mono text-sm bg-green-50 text-green-700 px-2 py-1 rounded inline-block">
                                            {rec.work.iswc}
                                        </span>
                                    ) : (
                                        <span className="text-slate-400 italic">Unlinked</span>
                                    )}
                                </td>
                                <td className="p-4 text-xs text-slate-500">
                                    {rec.updatedAt ? new Date(rec.updatedAt).toLocaleString() : '-'}
                                </td>
                                <td className="p-4">
                                    <StatusBadge status={rec.status || (rec.work ? 'HEALED' : 'ORPHANED')} />
                                </td>
                                <td className="p-4">
                                    {!rec.work && (
                                        <button
                                            onClick={() => handleOpenModal(rec)}
                                            className="text-xs bg-slate-100 hover:bg-blue-100 text-slate-600 hover:text-blue-700 px-3 py-1 rounded font-bold transition"
                                        >
                                            Link Manually
                                        </button>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <ManualLinkModal
                isOpen={modalOpen}
                onClose={() => setModalOpen(false)}
                recording={selectedRecording}
                onLinkSuccess={handleLinkSuccess}
            />

            {/* Custom Delete Confirmation Modal */}
            {deleteConfirmOpen && (
                <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-2xl p-6 max-w-sm w-full mx-4 animate-in fade-in zoom-in duration-200">
                        <div className="flex flex-col items-center text-center">
                            <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mb-4 text-red-600">
                                <Trash2 size={24} />
                            </div>
                            <h3 className="text-lg font-bold text-slate-800 mb-2">Delete Records?</h3>
                            <p className="text-slate-500 mb-6">
                                Are you sure you want to delete <span className="font-bold text-slate-800">{selectedIds.length}</span> selected records? This action cannot be undone.
                            </p>
                            <div className="flex gap-3 w-full">
                                <button
                                    onClick={() => setDeleteConfirmOpen(false)}
                                    className="flex-1 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg font-medium transition"
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={confirmDelete}
                                    className="flex-1 px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg font-medium shadow-sm transition"
                                >
                                    Yes, Delete
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}
