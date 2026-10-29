import { useEffect } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router';
import { useAuth } from '../auth/AuthContext';

export default function Layout() {
  const { isAuthenticated, signOut } = useAuth();
  const { pathname } = useLocation();

  // A new page starts at its top rather than where the previous page was scrolled to.
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  return (
    <>
      <header className="header">
        <span className="brand">Smart Health</span>
        {isAuthenticated && (
          <>
            <nav className="nav" aria-label="Main">
              <NavLink to="/appointments" className="nav-link">Appointments</NavLink>
              <NavLink to="/doctors" className="nav-link">Find a doctor</NavLink>
              <NavLink to="/profile" className="nav-link">Profile</NavLink>
            </nav>
            <button type="button" className="button button-secondary" onClick={signOut}>
              Sign out
            </button>
          </>
        )}
      </header>
      <main className="main">
        <Outlet />
      </main>
    </>
  );
}
