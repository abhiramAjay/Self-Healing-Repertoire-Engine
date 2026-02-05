import { NavLink } from 'react-router-dom';
import { Database, Activity, DollarSign, LayoutDashboard } from 'lucide-react';

const Sidebar = () => (
    <div className="w-64 bg-slate-900 h-screen text-white p-6 fixed">
        <h1 className="text-xl font-bold mb-10 flex items-center gap-2">
            <Database className="text-blue-400" /> Repertoire AI
        </h1>
        <nav className="space-y-4">
            <NavLink to="/" className={({ isActive }) =>
                `flex items-center gap-3 w-full p-2 rounded-lg transition ${isActive ? 'bg-blue-600' : 'text-slate-400 hover:text-white'}`
            }>
                <LayoutDashboard size={20} /> Dashboard
            </NavLink>
            <NavLink to="/log" className={({ isActive }) =>
                `flex items-center gap-3 w-full p-2 rounded-lg transition ${isActive ? 'bg-blue-600' : 'text-slate-400 hover:text-white'}`
            }>
                <Activity size={20} /> Integrity Log
            </NavLink>
            <NavLink to="/revenue" className={({ isActive }) =>
                `flex items-center gap-3 w-full p-2 rounded-lg transition ${isActive ? 'bg-blue-600' : 'text-slate-400 hover:text-white'}`
            }>
                <DollarSign size={20} /> Revenue Recovery
            </NavLink>
        </nav>
    </div>
);

export default Sidebar;
