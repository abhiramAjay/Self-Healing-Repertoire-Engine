import { X, Search } from 'lucide-react';
import { useState } from 'react';

export default function ManualLinkModal({ isOpen, onClose, recording, onLinkSuccess }) {
    const [searchTerm, setSearchTerm] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [searching, setSearching] = useState(false);

    const searchWorks = async () => {
        if (searchTerm.length > 2) {
            setSearching(true);
            try {
                const res = await fetch(`${import.meta.env.VITE_API_URL}/repertoire/works/search?query=${encodeURIComponent(searchTerm)}`);
                if (res.ok) {
                    const data = await res.json();
                    setSearchResults(data);
                }
            } catch (err) {
                console.error("Search failed", err);
            } finally {
                setSearching(false);
            }
        }
    };

    const handleLink = async (workId) => {
        try {
            await fetch(`${import.meta.env.VITE_API_URL}/repertoire/recordings/${recording.id}/link/${workId}`, {
                method: 'PUT'
            });
            onLinkSuccess();
            onClose();
        } catch (err) {
            console.error("Link failed", err);
        }
    };

    if (!isOpen || !recording) return null;

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-2xl w-full max-w-lg shadow-2xl overflow-hidden">
                <div className="p-6 border-b border-slate-100 flex justify-between items-center">
                    <h3 className="text-xl font-bold text-slate-900">Manually Link Recording</h3>
                    <button onClick={onClose} className="text-slate-400 hover:text-slate-600 transition">
                        <X size={24} />
                    </button>
                </div>

                <div className="p-6">
                    <p className="text-sm text-slate-500 mb-4">
                        Linking: <span className="font-bold text-slate-900">{recording.recordingTitle}</span>
                    </p>
                    <div className="relative mb-6">
                        <input
                            className="w-full pl-10 pr-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                            placeholder="Search by Song Title or ISWC..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && searchWorks()}
                        />
                        <Search className="absolute left-3 top-2.5 text-slate-400" size={18} />
                    </div>

                    <button
                        onClick={searchWorks}
                        disabled={searching || !searchTerm.trim()}
                        className="w-full mb-4 bg-blue-600 hover:bg-blue-700 disabled:bg-slate-300 text-white py-2 rounded-lg font-medium transition"
                    >
                        {searching ? 'Searching...' : 'Search Works'}
                    </button>

                    <div className="max-h-60 overflow-y-auto space-y-2">
                        {searchResults.length === 0 && searchTerm && !searching && (
                            <p className="text-center text-slate-400 py-8 italic">No works found. Try a different search term.</p>
                        )}
                        {searchResults.map(work => (
                            <button
                                key={work.id}
                                onClick={() => handleLink(work.id)}
                                className="w-full text-left p-3 rounded-lg hover:bg-blue-50 border border-slate-200 hover:border-blue-300 transition"
                            >
                                <p className="font-bold text-slate-800">{work.title}</p>
                                <p className="text-xs font-mono text-blue-600 mt-1">{work.iswc}</p>
                            </button>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}
