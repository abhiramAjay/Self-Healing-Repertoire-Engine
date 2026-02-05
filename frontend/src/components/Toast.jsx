import { useEffect } from 'react';
import { X, CheckCircle, AlertCircle } from 'lucide-react';

export default function Toast({ message, type = 'success', onClose, duration = 4000 }) {
    useEffect(() => {
        if (message) {
            const timer = setTimeout(() => {
                onClose();
            }, duration);
            return () => clearTimeout(timer);
        }
    }, [message, duration, onClose]);

    if (!message) return null;

    const styles = {
        success: { bg: 'bg-slate-900', icon: <CheckCircle className="text-green-400" size={20} />, border: 'border-green-500/30' },
        error: { bg: 'bg-slate-900', icon: <AlertCircle className="text-red-400" size={20} />, border: 'border-red-500/30' },
        info: { bg: 'bg-slate-900', icon: <CheckCircle className="text-blue-400" size={20} />, border: 'border-blue-500/30' }
    };

    const style = styles[type] || styles.info;

    return (
        <div className={`fixed bottom-6 right-6 z-50 flex items-center gap-4 px-6 py-4 
                        ${style.bg} text-white rounded-xl shadow-2xl border ${style.border} 
                        animate-slide-up backdrop-blur-sm bg-opacity-95`}>
            {style.icon}
            <div className="flex-1 mr-2">
                <p className="font-medium text-sm">{message}</p>
            </div>
            <button
                onClick={onClose}
                className="p-1 hover:bg-white/10 rounded-full transition-colors text-slate-400 hover:text-white"
            >
                <X size={18} />
            </button>
        </div>
    );
}
