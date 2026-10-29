import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { doctorApi } from '../api/doctorApi';
import { tokenStorage } from '../api/tokenStorage';
import DoctorForm from '../components/DoctorForm';
import ErrorMessage from '../components/ErrorMessage';

export default function DoctorEditPage() {
  const navigate = useNavigate();
  const userId = tokenStorage.getUserId();
  const [doctor, setDoctor] = useState(null);
  const [loadError, setLoadError] = useState(null);

  useEffect(() => {
    let current = true;
    doctorApi.getDoctor(userId)
      .then((loaded) => current && setDoctor(loaded))
      .catch((error) => current && setLoadError(error));
    return () => {
      current = false;
    };
  }, [userId]);

  async function save(profile, licenseFile) {
    await doctorApi.updateDoctor(userId, profile);
    if (licenseFile) {
      await doctorApi.updateLicenseFile(userId, licenseFile);
    }
    navigate('/profile', { replace: true });
  }

  if (loadError) {
    return <section className="card"><ErrorMessage error={loadError} /></section>;
  }
  if (!doctor) {
    return <section className="card"><p className="hint">Loading your doctor profile…</p></section>;
  }
  return (
    <section className="card">
      <h1>Edit doctor profile</h1>
      <DoctorForm doctor={doctor} submitLabel="Save" submittingLabel="Saving…" onSubmit={save}
                  onCancel={() => navigate('/profile')} />
    </section>
  );
}
