import { Link } from 'react-router';
import { DAYS_OF_WEEK, formatDayOfWeek } from '../utils/dates';

const initials = (doctor) => `${doctor.firstName.charAt(0)}${doctor.lastName.charAt(0)}`;

/** A doctor in a list, with a link to book an appointment unless it is the signed-in user. */
export default function DoctorCard({ doctor, isYou }) {
  const workingDays = DAYS_OF_WEEK.filter((day) => !doctor.daysOff.includes(day))
    .map((day) => formatDayOfWeek(day).slice(0, 3));
  return (
    <article className="doctor-card">
      <div className="doctor-card-header">
        <span className="avatar" aria-hidden="true">{initials(doctor)}</span>
        <div>
          <h2 className="doctor-name">Dr. {doctor.firstName} {doctor.lastName}</h2>
          <p className="doctor-meta">{doctor.address.city}, {doctor.address.country}</p>
        </div>
      </div>
      <div className="chips chips-static">
        {doctor.specializations.map((specialization) => (
          <span key={specialization.id} className="chip">{specialization.name}</span>
        ))}
      </div>
      <p className="doctor-meta">
        {workingDays.join(', ')} · {doctor.workingHours.start} – {doctor.workingHours.end}
      </p>
      {isYou
        ? <p className="doctor-meta doctor-you">This is you</p>
        : <Link to={`/doctors/${doctor.userId}/book`} className="button">Book an appointment</Link>}
    </article>
  );
}
