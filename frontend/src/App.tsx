import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AppProvider } from './store/AppContext';
import { Navbar } from './components/layout/Navbar';
import { Sidebar } from './components/layout/Sidebar';

// Public Pages (F0, F1, F2, F3, F4, F6)
import { Home } from './pages/public/Home';
import { Dashboard } from './pages/public/Dashboard';
import { AirQuality } from './pages/public/AirQuality';
import { WeatherSpatial } from './pages/public/WeatherSpatial';
import { PollutionMap } from './pages/public/PollutionMap';
import { Forecast } from './pages/public/Forecast';
import { Hotspots } from './pages/public/Hotspots';
import { CitizenReport } from './pages/public/CitizenReport';

// Analyst Pages (F5 Evidence & AI Reasoning)
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
import { ErrorBoundary } from './components/common/ErrorBoundary';

export const App: React.FC = () => {
  return (
    <ErrorBoundary>
      <AppProvider>
        <Router>
          <div className="app-shell">
          {/* F0 Sidebar Navigation */}
          <Sidebar />

          {/* F0 App Main (Navbar + Page Content) */}
          <div className="app-main">
            <Navbar />
            <main style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
              <Routes>
                {/* Landing & Foundation */}
                <Route path="/" element={<Home />} />
                <Route path="/home" element={<Home />} />

                {/* Command Center & F1 City + Air Quality */}
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/air-quality" element={<AirQuality />} />

                {/* F2 Weather & H3 Spatial Layer */}
                <Route path="/weather" element={<WeatherSpatial />} />
                <Route path="/spatial" element={<WeatherSpatial />} />
                <Route path="/map" element={<PollutionMap />} />

                {/* F3 Hotspots & F4 Forecast */}
                <Route path="/hotspots" element={<Hotspots />} />
                <Route path="/forecast" element={<Forecast />} />

                {/* F6 Citizen Reporting & Evidence Upload */}
                <Route path="/citizen/report" element={<CitizenReport />} />

                {/* F5 Evidence & Gemini WHY Intelligence */}
                <Route path="/analyst/evidence" element={<EvidenceAnalysis />} />
                <Route path="/gemini-why" element={<EvidenceAnalysis />} />

                {/* Analyst Routes */}
                <Route path="/analyst" element={<AnalystDashboard />} />
                <Route path="/analyst/hotspots" element={<HotspotAnalysis />} />
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
        </div>
      </Router>
    </AppProvider>
  </ErrorBoundary>
  );
};

export default App;
