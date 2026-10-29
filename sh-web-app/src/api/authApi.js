import { authRequest, publicRequest } from './client';

export const authApi = {
  createUser: (account) => publicRequest('/user-service/api/v1/users', { method: 'POST', body: account }),

  createSession: (credentials) => publicRequest('/user-service/api/v1/sessions', { method: 'POST', body: credentials }),

  deleteSession: (sessionId) => authRequest(`/user-service/api/v1/sessions/${sessionId}`, { method: 'DELETE' }),
};
