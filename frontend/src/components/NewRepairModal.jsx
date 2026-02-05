import { useState } from 'react';
import { X, Upload, Search } from 'lucide-react';

export default function NewRepairModal({ isOpen, onClose, onRefresh }) {
    const [activeTab, setActiveTab] = useState('single');
    const [singleData, setSingleData] = useState({ title: '', artist: '', isrc: '' });
    const [file, setFile] = useState(null);
    const [loading, setLoading] = useState(false);
    const [triggerHealing, setTriggerHealing] = useState(true); // New state for healing option

    const handleSingleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            await fetch(`${import.meta.env.VITE_API_URL}/repair/single?heal=${triggerHealing}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(singleData),
            });
            if (onRefresh) onRefresh();
            onClose();
        } catch (err) {
            console.error('Single repair failed', err);
        } finally {
            setLoading(false);
        }
    };

    const handleFileSubmit = async () => {
        if (!file) return;

        setLoading(true);
        try {
            const formData = new FormData();
            formData.append('file', file);
            await fetch(`${import.meta.env.VITE_API_URL}/repair/batch?heal=${triggerHealing}`, {
                method: 'POST',
                body: formData,
            });
            if (onRefresh) onRefresh();
            onClose();
        } catch (err) {
            console.error('Batch repair failed', err);
        } finally {
            setLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-2xl w-full max-w-xl shadow-2xl overflow-hidden">
                <div className="flex justify-between items-center p-6 border-b">
                    <h2 className="text-xl font-bold text-slate-900">New Repair Request</h2>
                    <button onClick={onClose} className="text-slate-400 hover:text-slate-600 transition">
                        <X size={24} />
                    </button>
                </div>

                <div className="flex border-b">
                    <button
                        onClick={() => setActiveTab('single')}
                        className={`flex-1 p-4 font-bold transition ${activeTab === 'single'
                            ? 'border-b-2 border-blue-600 text-blue-600'
                            : 'text-slate-400 hover:text-slate-600'
                            }`}
                    >
                        Single Record
                    </button>
                    <button
                        onClick={() => setActiveTab('batch')}
                        className={`flex-1 p-4 font-bold transition ${activeTab === 'batch'
                            ? 'border-b-2 border-blue-600 text-blue-600'
                            : 'text-slate-400 hover:text-slate-600'
                            }`}
                    >
                        Batch CSV Upload
                    </button>
                </div>

                <div className="p-8">
                    {/* Healing Option Checkbox */}
                    <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                        <label className="flex items-center gap-3 cursor-pointer">
                            <input
                                type="checkbox"
                                checked={triggerHealing}
                                onChange={(e) => setTriggerHealing(e.target.checked)}
                                className="w-5 h-5 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                            />
                            <div>
                                <span className="font-semibold text-slate-900">Trigger Healing Immediately</span>
                                <p className="text-xs text-slate-600 mt-1">
                                    {triggerHealing
                                        ? '✨ Will automatically discover and link ISWCs from MusicBrainz'
                                        : '📥 Will only import data without healing (can heal later)'}
                                </p>
                            </div>
                        </label>
                    </div>

                    {activeTab === 'single' ? (
                        <form onSubmit={handleSingleSubmit} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">Song Title *</label>
                                <input
                                    placeholder="Enter song title"
                                    className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                                    onChange={e => setSingleData({ ...singleData, title: e.target.value })}
                                    required
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">Artist Name</label>
                                <input
                                    placeholder="Enter artist name"
                                    className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                                    onChange={e => setSingleData({ ...singleData, artist: e.target.value })}
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-2">Known ISRC (Optional)</label>
                                <input
                                    placeholder="e.g., USRC12345678"
                                    className="w-full p-3 border border-slate-300 rounded-lg font-mono focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                                    onChange={e => setSingleData({ ...singleData, isrc: e.target.value })}
                                />
                            </div>
                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full bg-blue-600 text-white p-3 rounded-lg font-bold hover:bg-blue-700 disabled:bg-slate-300 transition"
                            >
                                {loading ? 'Processing...' : (triggerHealing ? 'Import & Heal' : 'Import Only')}
                            </button>
                        </form>
                    ) : (
                        <div className="text-center">
                            <label className="border-2 border-dashed border-slate-300 rounded-xl p-10 block cursor-pointer hover:border-blue-400 hover:bg-blue-50 transition">
                                <Upload className="mx-auto text-slate-400 mb-3" size={40} />
                                <span className="text-slate-600 font-medium block mb-1">
                                    {file ? file.name : 'Drop CSV file here or click to browse'}
                                </span>
                                <span className="text-xs text-slate-400">
                                    CSV should have columns: title, artist, isrc
                                </span>
                                <input
                                    type="file"
                                    accept=".csv"
                                    className="hidden"
                                    onChange={e => setFile(e.target.files[0])}
                                />
                            </label>
                            <button
                                onClick={handleFileSubmit}
                                disabled={!file || loading}
                                className="w-full mt-6 bg-indigo-600 text-white p-3 rounded-lg font-bold hover:bg-indigo-700 disabled:bg-slate-300 transition"
                            >
                                {loading ? 'Processing...' : (triggerHealing ? 'Import & Heal Batch' : 'Import Batch Only')}
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
