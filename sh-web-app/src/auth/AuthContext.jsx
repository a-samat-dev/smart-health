import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { authApi } from '../api/authApi';
import { tokenStorage } from '../api/tokenStorage';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [isAuthenticated, setAuthenticated] = useState(tokenStorage.hasTokens);

  useEffect(() => {
    const update = () => setAuthenticated(tokenStorage.hasTokens());
    const unsubscribe = tokenStorage.subscribe(update);
    // Signing in or out in another tab changes localStorage there; follow it here.
    window.addEventListener('storage', update);
    return () => {
      unsubscribe();
      window.removeEventListener('storage', update);
    };
  }, []);

  const value = useMemo(() => ({
    isAuthenticated,

    // Creates the account, then signs in with it.
    signUp: async (account) => {
      await authApi.createUser(account);
      tokenStorage.save(await authApi.createSession({ email: account.email, password: account.password }));
    },

    signIn: async (credentials) => tokenStorage.save(await authApi.createSession(credentials)),

    signOut: async () => {
      // Ending the session on the server needs the access token, so it happens before the tokens are
      // cleared. It is best effort: this device is signed out either way.
      const sessionId = tokenStorage.getSessionId();
      if (sessionId) {
        await authApi.deleteSession(sessionId).catch(() => {});
      }
      tokenStorage.clear();
    },
  }), [isAuthenticated]);

  return <AuthContext value={value}>{children}</AuthContext>;
}

export function useAuth() {
  return useContext(AuthContext);
}
