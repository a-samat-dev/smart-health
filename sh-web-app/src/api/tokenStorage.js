// The session lives in localStorage, so reloading the page keeps the user signed in.
const SESSION_ID_KEY = 'sh.sessionId';
const USER_ID_KEY = 'sh.userId';
const ACCESS_TOKEN_KEY = 'sh.accessToken';
const REFRESH_TOKEN_KEY = 'sh.refreshToken';
const KEYS = [SESSION_ID_KEY, USER_ID_KEY, ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY];

const listeners = new Set();

function notify() {
  listeners.forEach((listener) => listener());
}

export const tokenStorage = {
  getSessionId: () => localStorage.getItem(SESSION_ID_KEY),

  getUserId: () => localStorage.getItem(USER_ID_KEY),

  getAccessToken: () => localStorage.getItem(ACCESS_TOKEN_KEY),

  getRefreshToken: () => localStorage.getItem(REFRESH_TOKEN_KEY),

  // A session saved by an older version of the app may lack some of these; it then counts as signed out.
  hasTokens: () => KEYS.every((key) => localStorage.getItem(key) !== null),

  /** Stores a session as returned by the sessions API. */
  save({ id, userId, accessToken, refreshToken }) {
    localStorage.setItem(SESSION_ID_KEY, id);
    localStorage.setItem(USER_ID_KEY, userId);
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    notify();
  },

  clear() {
    KEYS.forEach((key) => localStorage.removeItem(key));
    notify();
  },

  /** Calls the listener whenever tokens are saved or cleared. Returns a function that unsubscribes. */
  subscribe(listener) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
};
