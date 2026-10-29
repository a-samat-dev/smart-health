import { authRequest, withQuery } from './client';

// The largest page the API allows.
const MAX_PAGE_SIZE = 100;

export const appointmentApi = {
  /** The doctor's slots between two Dates, each with whether it is still free. */
  getSlots: (doctorId, from, to) =>
    authRequest(withQuery('/appointment-service/api/v1/slots', { doctorId, from: from.toISOString(), to: to.toISOString() })),

  createAppointment: (appointment) => authRequest('/appointment-service/api/v1/appointments', { method: 'POST', body: appointment }),

  /**
   * One page of the user's appointments. Filters: cursor (nextCursor of the previous page), size,
   * specialization (text), from and to (Dates the appointments start between), and sort, such as
   * "startTime,asc".
   */
  getAppointments: ({ from, to, ...filters } = {}) =>
    authRequest(withQuery('/appointment-service/api/v1/appointments', {
      ...filters,
      from: from?.toISOString(),
      to: to?.toISOString(),
    })),

  /** Every appointment that starts between two Dates, earliest first, for the calendar views. */
  async getAllAppointments(from, to) {
    const appointments = [];
    let cursor;
    do {
      const page = await appointmentApi.getAppointments({ from, to, cursor, size: MAX_PAGE_SIZE, sort: 'startTime,asc' });
      appointments.push(...page.items);
      cursor = page.nextCursor;
    } while (cursor);
    return appointments;
  },

  getAppointment: (id) => authRequest(`/appointment-service/api/v1/appointments/${id}`),

  updateAppointment: (id, changes) => authRequest(`/appointment-service/api/v1/appointments/${id}`, { method: 'PUT', body: changes }),

  deleteAppointment: (id) => authRequest(`/appointment-service/api/v1/appointments/${id}`, { method: 'DELETE' }),
};
