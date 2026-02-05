import React from 'react';
import { DollarSign } from 'lucide-react';

const RevenueWidget = ({ revenue }) => {
    return (
        <div className="bg-gradient-to-r from-emerald-500 to-teal-600 p-6 rounded-xl shadow-lg text-white mb-8">
            <div className="flex justify-between items-center">
                <div>
                    <p className="text-emerald-100 text-sm font-medium mb-1">Estimated Revenue Recovered</p>
                    <p className="text-4xl font-bold">${revenue.toFixed(2)}</p>
                    <p className="text-emerald-100 text-xs mt-2">*Based on $0.05 avg royalty per healed track</p>
                </div>
                <div className="bg-white/20 p-4 rounded-full">
                    <DollarSign className="w-8 h-8 text-white" />
                </div>
            </div>
        </div>
    );
};

export default RevenueWidget;
