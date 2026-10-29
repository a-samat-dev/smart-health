import { tokenStorage } from './tokenStorage';

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

/** A failed request, with the status and the message from the ApiError body the services return. */
export class ApiError extends Error {
  constructor(status, message, details = []) {
    super(message);
    this.status = status;
    this.details = details;
  }
}

let refreshInFlight = null;

/** Sends a request without an access token: for sign-up, sign-in and token refresh. */
export async function publicRequest(path, options) {
  return parse(await send(path, options));
}

/**
 * Sends a request with the access token. If the gateway rejects the token, because it expired after
 * an hour, the tokens are refreshed and the request is sent once more.
 */
export async function authRequest(path, options) {
  const accessToken = tokenStorage.getAccessToken();
  let response = await send(path, options, accessToken);

  if (response.status === 401 && tokenStorage.hasTokens()) {
    // Skip the refresh if another request already replaced the token while this one was in flight.
    if (tokenStorage.getAccessToken() === accessToken) {
      await refreshTokens();
    }
    response = await send(path, options, tokenStorage.getAccessToken());
  }
  return parse(response, options);
}

/** Concurrent callers share one refresh, because each refresh token can be used only once. */
function refreshTokens() {
  refreshInFlight ??= exchangeRefreshToken().finally(() => {
    refreshInFlight = null;
  });
  return refreshInFlight;
}

async function exchangeRefreshToken() {
  const sessionId = tokenStorage.getSessionId();
  const refreshToken = tokenStorage.getRefreshToken();
  try {
    tokenStorage.save(await publicRequest(`/user-service/api/v1/sessions/${sessionId}/tokens`, {
      method: 'POST',
      body: { refreshToken },
    }));
  } catch (error) {
    // A rejected refresh token ends the session, which signs the user out. The tokens stay when the
    // server was only unreachable, or when another tab has already stored a newer refresh token.
    if (error.status === 401 && tokenStorage.getRefreshToken() === refreshToken) {
      tokenStorage.clear();
    }
    throw error;
  }
}

/** Adds the parameters that have a value to the path as a query string. */
export function withQuery(path, params) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([name, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.set(name, value);
    }
  });
  const text = query.toString();
  return text ? `${path}?${text}` : path;
}

// A FormData body, such as a file upload, is sent as multipart/form-data; fetch sets that header itself.
async function send(path, { method = 'GET', body } = {}, accessToken = null) {
  const headers = {};
  const isForm = body instanceof FormData;
  if (body !== undefined && !isForm) {
    headers['Content-Type'] = 'application/json';
  }
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }

  try {
    return await fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body === undefined || isForm ? body : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Check your connection and try again.');
  }
}

async function parse(response, { responseType = 'json' } = {}) {
  if (response.status === 204) {
    return null;
  }
  if (response.ok && responseType === 'blob') {
    return response.blob();
  }
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(response.status, body?.message ?? `Request failed with status ${response.status}`, body?.details);
  }
  return body;
}
