import { useState, useEffect } from 'react';
import { Plus } from 'lucide-react';
import StatsGrid from '../components/StatsGrid';
import HealerControl from '../components/HealerControl';
import AuditTable from '../components/AuditTable';
import NewRepairModal from '../components/NewRepairModal';
import Toast from '../components/Toast';

export default function Dashboard() {
    const [stats, setStats] = useState({ totalRecordings: 0, orphanedCount: 0, healedCount: 0, estimatedRecovery: 0 });
    const [recordings, setRecordings] = useState([]);
    const [loading, setLoading] = useState(false);
    const [repairModalOpen, setRepairModalOpen] = useState(false);

    // Toast State
    const [toast, setToast] = useState({ message: null, type: 'success' });

    const showToast = (message, type = 'success') => {
        setToast({ message, type });
    };

    const closeToast = () => setToast({ ...toast, message: null });

    const fetchStats = async () => {
        try {
            const res = await fetch(`${import.meta.env.VITE_API_URL}/stats`);
            if (res.ok) setStats(await res.json());
        } catch (e) {
            console.error("Failed to fetch stats", e);
        }
    };

    const fetchRecordings = async () => {
        setIsLoading(true);
        try {
            const recRes = await fetch(`${import.meta.env.VITE_API_URL}/recordings`);
            if (recRes.ok) setRecordings(await recRes.json());
        } catch (e) {
            console.error("Failed to fetch recordings", e);
        } finally {
            setIsLoading(false);
        }
    };

    const fetchData = async () => {
        await fetchStats();
        await fetchRecordings();
    };

    useEffect(() => {
        fetchData();
    }, []);

    const [selectedIds, setSelectedIds] = useState([]);

    const triggerHeal = async () => {
        setIsHealing(true);
        try {
            const res = await fetch(`${import.meta.env.VITE_API_URL}/heal-now`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(selectedIds)
            });

            if (res.status === 202) {
                const msg = selectedIds.length > 0
                    ? `🚀 Healing job started for ${selectedIds.length} records!`
                    : "🚀 Full catalog healing started!";
                showToast(msg, "success");

                // Poll for updates every 2 seconds for 10 seconds
                let attempts = 0;
                const interval = setInterval(async () => {
                    await fetchData();
                    attempts++;
                    if (attempts >= 5) clearInterval(interval);
                }, 2000);
            } else {
                showToast("Heal trigger failed. Check server logs.", "error");
            }
        } catch (err) {
            console.error("Healing trigger failed", err);
            showToast("Network error. Is the backend running?", "error");
        }
        setLoading(false);
    };

    return (
        <div className="p-8">
            <header className="flex justify-between items-center mb-8">
                <div>
                    <h2 className="text-3xl font-bold text-slate-900">Catalog Health</h2>
                    <p className="text-slate-500">Monitor and repair ISRC/ISWC metadata integrity.</p>
                </div>
                <div className="flex gap-3">
                    <button
                        onClick={() => setRepairModalOpen(true)}
                        className="flex items-center gap-2 px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg font-medium transition shadow-lg"
                    >
                        <Plus size={20} /> New Repair
                    </button>
                    <HealerControl onHeal={triggerHeal} loading={loading} />
                </div>
            </header>

            <StatsGrid stats={stats} />
            <AuditTable
                recordings={recordings}
                onRefresh={fetchData}
                onSelectionChange={setSelectedIds}
            />

            <NewRepairModal
                isOpen={repairModalOpen}
                onClose={() => setRepairModalOpen(false)}
                onRefresh={() => {
                    fetchData();
                    showToast("Record imported successfully!", "success");
                }}
            />

            <Toast
                message={toast.message}
                type={toast.type}
                onClose={closeToast}
            />
        </div>
    );
}
