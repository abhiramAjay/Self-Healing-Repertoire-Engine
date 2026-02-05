import { Zap, Loader2 } from 'lucide-react';

export default function HealerControl({ onHeal, loading }) {
    return (
        <button
            onClick={onHeal}
            disabled={loading}
            className={`flex items-center gap-2 px-6 py-3 rounded-lg font-bold text-white transition shadow-lg ${loading ? 'bg-slate-400 cursor-not-allowed' : 'bg-indigo-600 hover:bg-indigo-700 active:scale-95'
                }`}
        >
            {loading ? <Loader2 className="animate-spin" size={20} /> : <Zap size={20} />}
            {loading ? 'Healing Catalog...' : 'Trigger Healing Cycle'}
        </button>
    );
}
