import { useState, useEffect } from 'react';

export default function RevenueRecovery() {
    const [stats, setStats] = useState({ totalRecordings: 0, orphanedCount: 0, healedCount: 0, estimatedRecovery: 0 });
    const [recordings, setRecordings] = useState([]);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const statsRes = await fetch(`${import.meta.env.VITE_API_URL}/stats`);
                const statsData = await statsRes.json();
                setStats(statsData);

                // Fetch recordings to calculate potential recovery
                const recRes = await fetch(`${import.meta.env.VITE_API_URL}/recordings`);
                if (recRes.ok) {
                    const recData = await recRes.json();
                    setRecordings(recData);
                }
            } catch (err) {
                console.error("Failed to fetch data", err);
            }
        };
        fetchData();
    }, []);

    const orphaned = recordings.filter(r => r.work == null);
    // Estimate trapped revenue: orphaned records * 1000 simulated streams * industry rate
    const trappedRevenue = orphaned.length * 1000 * 0.004;

    return (
        <div className="p-8">
            <h2 className="text-2xl font-bold mb-6 text-slate-900">Financial Recovery Analysis</h2>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8">
                <div className="bg-indigo-900 text-white p-6 rounded-2xl shadow-xl">
                    <p className="text-indigo-200 text-sm uppercase tracking-wider font-semibold">Recovered to Date</p>
                    <p className="text-4xl font-bold mt-2">${stats.estimatedRecovery.toFixed(2)}</p>
                    <div className="mt-4 h-2 bg-indigo-800 rounded-full overflow-hidden">
                        <div className="h-full bg-green-400" style={{ width: '65%' }}></div>
                    </div>
                </div>

                <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
                    <p className="text-slate-500 text-sm uppercase tracking-wider font-semibold">Trapped "Black Box" Revenue</p>
                    <p className="text-4xl font-bold mt-2 text-slate-800">${trappedRevenue.toFixed(2)}</p>
                    <p className="text-xs text-red-500 mt-2 font-medium">● Requires Metadata Repair</p>
                </div>
            </div>

            <div className="bg-white rounded-xl border border-slate-200 p-6">
                <h3 className="font-bold text-slate-800 mb-4">Recovery Breakdown</h3>
                <div className="space-y-4">
                    <div className="flex justify-between items-center p-3 bg-slate-50 rounded-lg">
                        <span className="text-slate-600">Total Recordings</span>
                        <span className="font-bold text-slate-900">{stats.totalRecordings}</span>
                    </div>
                    <div className="flex justify-between items-center p-3 bg-green-50 rounded-lg">
                        <span className="text-green-700">Healed & Recoverable</span>
                        <span className="font-bold text-green-900">{stats.healedCount}</span>
                    </div>
                    <div className="flex justify-between items-center p-3 bg-red-50 rounded-lg">
                        <span className="text-red-700">Still Orphaned</span>
                        <span className="font-bold text-red-900">{stats.orphanedCount}</span>
                    </div>
                </div>
                <p className="text-slate-500 text-sm italic mt-6">Detailed per-ISRC financial logs are currently being calculated...</p>
            </div>
        </div>
    );
}
