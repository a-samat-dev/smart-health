import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router';
import { doctorApi } from '../api/doctorApi';
import { tokenStorage } from '../api/tokenStorage';
import DoctorCard from '../components/DoctorCard';
import ErrorMessage from '../components/ErrorMessage';

// The specialization and page live in the URL, so going back from a booking returns to the same list.
export default function DoctorsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const specializationId = searchParams.get('specializationId') ?? '';
  const page = Number(searchParams.get('page') ?? 0);
  const [specializations, setSpecializations] = useState([]);
  const [doctors, setDoctors] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    doctorApi.getSpecializations().then(setSpecializations).catch(setError);
  }, []);

  useEffect(() => {
    let current = true;
    setDoctors(null);
    doctorApi.getDoctors({ specializationId, page })
      .then((loaded) => current && setDoctors(loaded))
      .catch((loadError) => current && setError(loadError));
    return () => {
      current = false;
    };
  }, [specializationId, page]);

  const show = (params) => setSearchParams(Object.fromEntries(
    Object.entries({ specializationId, page, ...params }).filter(([, value]) => value !== '' && value !== 0)));

  return (
    <section className="card card-wide">
      <div className="card-header">
        <h1>Find a doctor</h1>
        <label className="field field-inline">
          <span className="field-label">Specialization</span>
          <select className="field-input" value={specializationId}
                  onChange={(event) => show({ specializationId: event.target.value, page: 0 })}>
            <option value="">All specializations</option>
            {specializations.map((specialization) => (
              <option key={specialization.id} value={specialization.id}>{specialization.name}</option>
            ))}
          </select>
        </label>
      </div>
      <ErrorMessage error={error} />
      {!doctors && !error && <p className="hint">Loading doctors…</p>}
      {doctors?.items.length === 0 && <p className="empty-state">No doctors with this specialization yet.</p>}
      {doctors?.items.length > 0 && (
        <>
          <div className="doctor-grid">
            {doctors.items.map((doctor) => (
              <DoctorCard key={doctor.userId} doctor={doctor} isYou={doctor.userId === tokenStorage.getUserId()} />
            ))}
          </div>
          {doctors.totalPages > 1 && (
            <div className="pager">
              <button type="button" className="button button-secondary" disabled={page === 0}
                      onClick={() => show({ page: page - 1 })}>Previous</button>
              <span className="hint-flush">Page {page + 1} of {doctors.totalPages}</span>
              <button type="button" className="button button-secondary" disabled={page + 1 >= doctors.totalPages}
                      onClick={() => show({ page: page + 1 })}>Next</button>
            </div>
          )}
        </>
      )}
    </section>
  );
}
