import { Navigate, useLocation } from 'react-router';
import { useAuth } from './AuthContext';

/** Sends signed-out visitors to the sign-in page, remembering where they wanted to go. */
export function RequireAuth({ children }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/sign-in" replace state={{ from: location.pathname }} />;
  }
  return children;
}

/**
 * For the sign-in and sign-up pages: once the user is signed in, moves them on to where they wanted to go,
 * or else to redirectTo.
 */
export function GuestOnly({ children, redirectTo = '/appointments' }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (isAuthenticated) {
    return <Navigate to={location.state?.from ?? redirectTo} replace />;
  }
  return children;
}
