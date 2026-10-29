import { Navigate, Route, Routes } from 'react-router';
import { GuestOnly, RequireAuth } from './auth/RouteGuards';
import Layout from './components/Layout';
import AppointmentsPage from './pages/AppointmentsPage';
import BookingPage from './pages/BookingPage';
import DoctorEditPage from './pages/DoctorEditPage';
import DoctorRegistrationPage from './pages/DoctorRegistrationPage';
import DoctorsPage from './pages/DoctorsPage';
import ProfilePage from './pages/ProfilePage';
import SignInPage from './pages/SignInPage';
import SignUpPage from './pages/SignUpPage';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/sign-in" element={<GuestOnly><SignInPage /></GuestOnly>} />
        <Route path="/sign-up" element={<GuestOnly><SignUpPage /></GuestOnly>} />
        <Route path="/sign-up/doctor"
               element={<GuestOnly redirectTo="/doctor-registration"><SignUpPage asDoctor /></GuestOnly>} />
        <Route path="/profile" element={<RequireAuth><ProfilePage /></RequireAuth>} />
        <Route path="/doctor-registration" element={<RequireAuth><DoctorRegistrationPage /></RequireAuth>} />
        <Route path="/doctor-profile/edit" element={<RequireAuth><DoctorEditPage /></RequireAuth>} />
        <Route path="/doctors" element={<RequireAuth><DoctorsPage /></RequireAuth>} />
        <Route path="/doctors/:doctorId/book" element={<RequireAuth><BookingPage /></RequireAuth>} />
        <Route path="/appointments" element={<RequireAuth><AppointmentsPage /></RequireAuth>} />
        <Route path="*" element={<Navigate to="/appointments" replace />} />
      </Route>
    </Routes>
  );
}
