import React from 'react';

const StatsGrid = ({ stats }) => (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {[
            { label: 'Total Recordings', value: stats.totalRecordings, color: 'border-blue-500' },
            { label: 'Orphaned (No ISWC)', value: stats.orphanedCount, color: 'border-red-500' },
            { label: 'Healed Today', value: stats.healedCount, color: 'border-green-500' },
            { label: 'Revenue Recovered', value: `$${stats.estimatedRecovery.toFixed(2)}`, color: 'border-purple-500' },
        ].map((item, i) => (
            <div key={i} className={`bg-white p-6 rounded-xl border-l-4 shadow-sm ${item.color}`}>
                <p className="text-slate-500 text-sm font-medium">{item.label}</p>
                <p className="text-2xl font-bold text-slate-800">{item.value}</p>
            </div>
        ))}
    </div>
);

export default StatsGrid;
