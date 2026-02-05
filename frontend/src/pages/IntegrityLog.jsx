import { CheckCircle2, AlertCircle } from 'lucide-react';
import { useState, useEffect } from 'react';

export default function IntegrityLog() {
    const [recordings, setRecordings] = useState([]);

    useEffect(() => {
        const fetchRecordings = async () => {
            try {
                const res = await fetch(`${import.meta.env.VITE_API_URL}/recordings`);
                if (res.ok) {
                    const data = await res.json();
                    setRecordings(data);
                }
            } catch (err) {
                console.error("Failed to fetch recordings", err);
            }
        };
        fetchRecordings();
    }, []);

    // Only show recordings that have been healed
    const healedRecords = recordings.filter(r => r.work != null);

    return (
        <div className="p-8">
            <h2 className="text-2xl font-bold mb-6 text-slate-900">Integrity Audit Log</h2>
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                <div className="p-4 bg-slate-50 border-b border-slate-200 text-sm font-medium text-slate-500">
                    Recent Healing Actions
                </div>
                <div className="divide-y divide-slate-100">
                    {healedRecords.map((rec) => (
                        <div key={rec.id} className="p-4 flex items-center justify-between hover:bg-slate-50 transition">
                            <div className="flex items-center gap-4">
                                <CheckCircle2 className="text-green-500" size={20} />
                                <div>
                                    <p className="font-medium text-slate-800">Linked {rec.isrc || 'N/A'}</p>
                                    <p className="text-sm text-slate-500">Matched to Work: {rec.work.title}</p>
                                    {rec.discoverySource && (
                                        <p className="text-[10px] text-indigo-500 font-semibold uppercase mt-1">
                                            Source: {rec.discoverySource}
                                        </p>
                                    )}
                                </div>
                            </div>
                            <div className="text-right">
                                <span className="text-xs font-mono bg-blue-50 text-blue-600 px-2 py-1 rounded">
                                    {rec.work.iswc}
                                </span>
                                <p className="text-xs text-slate-400 mt-1">Status: Verified</p>
                            </div>
                        </div>
                    ))}
                    {healedRecords.length === 0 && (
                        <div className="p-8 text-center text-slate-400">
                            <AlertCircle className="mx-auto mb-2 opacity-20" size={40} />
                            <p>No healing actions recorded yet. Trigger a cycle to see results.</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
