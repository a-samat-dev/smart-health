import { authRequest } from './client';
import { tokenStorage } from './tokenStorage';

// The signed-in user's own account; its id comes with the session.
const ownUserPath = () => `/user-service/api/v1/users/${tokenStorage.getUserId()}`;

export const userApi = {
  getProfile: () => authRequest(ownUserPath()),

  updateProfile: (profile) => authRequest(ownUserPath(), { method: 'PUT', body: profile }),
};
