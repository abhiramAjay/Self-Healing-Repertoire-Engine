import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import Dashboard from './pages/Dashboard';
import IntegrityLog from './pages/IntegrityLog';
import RevenueRecovery from './pages/RevenueRecovery';

export default function App() {
  return (
    <BrowserRouter>
      <div className="flex bg-slate-50 min-h-screen font-sans">
        <Sidebar />
        <main className="ml-64 flex-1">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/log" element={<IntegrityLog />} />
            <Route path="/revenue" element={<RevenueRecovery />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
