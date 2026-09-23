import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Navbar } from './components/layout/Navbar';

// Public Pages
import { Home } from './pages/public/Home';
import { Dashboard } from './pages/public/Dashboard';
import { PollutionMap } from './pages/public/PollutionMap';
import { Forecast } from './pages/public/Forecast';
import { Hotspots } from './pages/public/Hotspots';
import { CitizenReport } from './pages/public/CitizenReport';

// Analyst Pages
import { AnalystDashboard } from './pages/analyst/AnalystDashboard';
import { HotspotAnalysis } from './pages/analyst/HotspotAnalysis';
import { EvidenceAnalysis } from './pages/analyst/EvidenceAnalysis';
import { ForecastAnalysis } from './pages/analyst/ForecastAnalysis';

// Authority Pages
import { AuthorityDashboard } from './pages/authority/AuthorityDashboard';
import { Alerts } from './pages/authority/Alerts';
import { IncidentQueuePage } from './pages/authority/IncidentQueue';
import { InspectionPage } from './pages/authority/Inspection';
import { ActionsPage } from './pages/authority/Actions';

// Admin Pages
import { AdminDashboard } from './pages/admin/AdminDashboard';
import { UsersPage } from './pages/admin/Users';
import { CitiesPage } from './pages/admin/Cities';
import { SystemStatus } from './pages/admin/SystemStatus';

// Federated Pages
import { FederatedNetwork } from './pages/federated/FederatedNetwork';

export const App: React.FC = () => {
  return (
    <Router>
      <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
        <Navbar />
        <main style={{ flex: 1 }}>
          <Routes>
            {/* Public Routes */}
            <Route path="/" element={<Home />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/map" element={<PollutionMap />} />
            <Route path="/forecast" element={<Forecast />} />
            <Route path="/hotspots" element={<Hotspots />} />
            <Route path="/citizen/report" element={<CitizenReport />} />

            {/* Analyst Routes */}
            <Route path="/analyst" element={<AnalystDashboard />} />
            <Route path="/analyst/hotspots" element={<HotspotAnalysis />} />
            <Route path="/analyst/evidence" element={<EvidenceAnalysis />} />
            <Route path="/analyst/forecast" element={<ForecastAnalysis />} />

            {/* Authority Routes */}
            <Route path="/authority" element={<AuthorityDashboard />} />
            <Route path="/authority/incidents" element={<IncidentQueuePage />} />
            <Route path="/authority/alerts" element={<Alerts />} />
            <Route path="/authority/inspection" element={<InspectionPage />} />
            <Route path="/authority/actions" element={<ActionsPage />} />

            {/* Admin Routes */}
            <Route path="/admin" element={<AdminDashboard />} />
            <Route path="/admin/users" element={<UsersPage />} />
            <Route path="/admin/cities" element={<CitiesPage />} />
            <Route path="/admin/status" element={<SystemStatus />} />

            {/* Federated Prototype */}
            <Route path="/federated" element={<FederatedNetwork />} />

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
};

export default App;
