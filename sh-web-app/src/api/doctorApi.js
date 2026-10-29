import { authRequest, withQuery } from './client';

// Registration and license uploads are multipart: the profile as a JSON part, the PDF as a file part.
function doctorForm(profile, licenseFile) {
  const form = new FormData();
  if (profile) {
    form.append('doctor', new Blob([JSON.stringify(profile)], { type: 'application/json' }));
  }
  form.append('licenseFile', licenseFile);
  return form;
}

export const doctorApi = {
  getSpecializations: () => authRequest('/doctor-service/api/v1/specializations'),

  createDoctor: (profile, licenseFile) =>
    authRequest('/doctor-service/api/v1/doctors', { method: 'POST', body: doctorForm(profile, licenseFile) }),

  getDoctors: ({ specializationId, page = 0, size = 12 } = {}) =>
    authRequest(withQuery('/doctor-service/api/v1/doctors', { specializationId, page, size })),

  getDoctor: (userId) => authRequest(`/doctor-service/api/v1/doctors/${userId}`),

  updateDoctor: (userId, profile) => authRequest(`/doctor-service/api/v1/doctors/${userId}`, { method: 'PUT', body: profile }),

  getLicenseFile: (userId) => authRequest(`/doctor-service/api/v1/doctors/${userId}/license-file`, { responseType: 'blob' }),

  updateLicenseFile: (userId, licenseFile) =>
    authRequest(`/doctor-service/api/v1/doctors/${userId}/license-file`, { method: 'PUT', body: doctorForm(null, licenseFile) }),
};
