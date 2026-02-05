import React from 'react';
import { Activity } from 'lucide-react';

const HealingLog = ({ report }) => {
    if (!report) return null;

    return (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
            <div className="p-4 border-b border-gray-100 bg-gray-50 flex items-center">
                <Activity className="w-4 h-4 text-gray-400 mr-2" />
                <h3 className="font-semibold text-gray-700">Recent Healing Activity</h3>
            </div>
            <div className="p-4">
                {Object.keys(report.details).length === 0 ? (
                    <p className="text-gray-500 text-sm italic">No attempts in last cycle.</p>
                ) : (
                    <ul className="space-y-3">
                        {Object.entries(report.details).map(([recording, status], index) => {
                            const isSuccess = status.includes("Healed");
                            return (
                                <li key={index} className="flex justify-between items-start text-sm">
                                    <span className="font-medium text-gray-800">{recording}</span>
                                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${isSuccess
                                            ? "bg-green-100 text-green-700"
                                            : "bg-gray-100 text-gray-600"
                                        }`}>
                                        {status}
                                    </span>
                                </li>
                            );
                        })}
                    </ul>
                )}
            </div>
            <div className="bg-gray-50 p-3 border-t border-gray-100 text-xs text-gray-500 flex justify-between">
                <span>Cycle Orphans: {report.totalOrphans}</span>
                <span>Matched: {report.healedByDirectMatch + report.healedByFuzzyMatch}</span>
            </div>
        </div>
    );
};

export default HealingLog;
