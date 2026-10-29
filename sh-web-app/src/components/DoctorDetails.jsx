import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { doctorApi } from '../api/doctorApi';
import { DAYS_OF_WEEK, formatDayOfWeek } from '../utils/dates';
import ErrorMessage from './ErrorMessage';

/** The signed-in doctor's own profile, with their license file. */
export default function DoctorDetails({ userId }) {
  const [doctor, setDoctor] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let current = true;
    doctorApi.getDoctor(userId)
      .then((loaded) => current && setDoctor(loaded))
      .catch((loadError) => current && setError(loadError));
    return () => {
      current = false;
    };
  }, [userId]);

  async function openLicense() {
    setError(null);
    try {
      const url = URL.createObjectURL(await doctorApi.getLicenseFile(userId));
      window.open(url, '_blank', 'noopener');
      // The new tab has loaded the file by then.
      setTimeout(() => URL.revokeObjectURL(url), 60 * 1000);
    } catch (openError) {
      setError(openError);
    }
  }

  if (!doctor) {
    return error ? <ErrorMessage error={error} /> : <p className="hint">Loading your doctor profile…</p>;
  }
  const workingDays = DAYS_OF_WEEK.filter((day) => !doctor.daysOff.includes(day)).map(formatDayOfWeek);
  return (
    <>
      <div className="card-header">
        <h2>Doctor profile</h2>
        <Link to="/doctor-profile/edit" className="button button-secondary">Edit doctor profile</Link>
      </div>
      <div className="chips chips-static">
        {doctor.specializations.map((specialization) => (
          <span key={specialization.id} className="chip">{specialization.name}</span>
        ))}
      </div>
      <dl className="details">
        <dt>License number</dt>
        <dd>
          {doctor.licenseNumber}{' '}
          <button type="button" className="link-button" onClick={openLicense}>View license</button>
        </dd>
        <dt>Practice</dt>
        <dd>{doctor.address.street} {doctor.address.houseNumber}, {doctor.address.city}, {doctor.address.country}</dd>
        <dt>Contact</dt>
        <dd>{doctor.contactEmail} · {doctor.contactPhone}</dd>
        <dt>Working hours</dt>
        <dd>{doctor.workingHours.start} – {doctor.workingHours.end} ({doctor.timeZone})</dd>
        <dt>Working days</dt>
        <dd>{workingDays.join(', ')}</dd>
      </dl>
      <ErrorMessage error={error} />
    </>
  );
}
