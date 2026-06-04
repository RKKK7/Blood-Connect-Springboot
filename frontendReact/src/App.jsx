import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/ui/Navbar';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import Requests from './pages/Requests';
import RequestDetail from './pages/RequestDetail';
import NewRequest from './pages/NewRequest';
import Leaderboard from './pages/Leaderboard';
import Notifications from './pages/Notifications';
import Profile from './pages/Profile';
import AdminDashboard from './pages/AdminDashboard';
import AdminAnalytics from './pages/AdminAnalytics';
import Shortage from './pages/Shortage';
import Stats from './pages/Stats';
import Chat from './pages/Chat';
import DonationHistory from './pages/DonationHistory';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';

const Protected = ({ children }) => {
  const { user, loading } = useAuth();
  if (loading) return null;
  if (!user) return <Navigate to="/login" />;
  return children;
};

const AdminRoute = ({ children }) => {
  const { user, loading } = useAuth();
  if (loading) return null;
  if (!user || user.role !== 'admin') return <Navigate to="/" />;
  return children;
};

function AppRoutes() {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/"                       element={<Home />} />
        <Route path="/login"                  element={<Login />} />
        <Route path="/register"               element={<Register />} />
        <Route path="/forgot-password"        element={<ForgotPassword />} />
        <Route path="/reset-password/:token"  element={<ResetPassword />} />
        <Route path="/requests"               element={<Requests />} />
        <Route path="/requests/new"           element={<Protected><NewRequest /></Protected>} />
        <Route path="/requests/:id"           element={<RequestDetail />} />
        <Route path="/leaderboard"            element={<Leaderboard />} />
        <Route path="/shortage"               element={<Shortage />} />
        <Route path="/stats"                  element={<Stats />} />
        <Route path="/chat/:donationId"       element={<Protected><Chat /></Protected>} />
        <Route path="/donations/history"      element={<Protected><DonationHistory /></Protected>} />
        <Route path="/notifications"          element={<Protected><Notifications /></Protected>} />
        <Route path="/profile"                element={<Protected><Profile /></Protected>} />
        <Route path="/admin"                  element={<AdminRoute><AdminDashboard /></AdminRoute>} />
        <Route path="/admin/analytics"        element={<AdminRoute><AdminAnalytics /></AdminRoute>} />
        <Route path="*"                       element={<Navigate to="/" />} />
      </Routes>
    </>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}
