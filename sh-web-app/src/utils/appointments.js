// How an appointment reads to the signed-in user, who may be its patient or its doctor.

/** The other person in the appointment: the doctor for its patient, the patient for its doctor. */
export function otherParty(appointment, userId) {
  return appointment.clientId === userId ? `Dr. ${appointment.doctorName}` : appointment.clientName;
}

/** "as-patient" for appointments the user booked, "as-doctor" for those booked with them. */
export function roleClass(appointment, userId) {
  return appointment.clientId === userId ? 'as-patient' : 'as-doctor';
}
