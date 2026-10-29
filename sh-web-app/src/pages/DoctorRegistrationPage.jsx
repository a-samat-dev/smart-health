import { useNavigate } from 'react-router';
import { doctorApi } from '../api/doctorApi';
import DoctorForm from '../components/DoctorForm';

export default function DoctorRegistrationPage() {
  const navigate = useNavigate();

  async function register(profile, licenseFile) {
    await doctorApi.createDoctor(profile, licenseFile);
    navigate('/profile', { replace: true });
  }

  return (
    <section className="card">
      <h1>Register as a doctor</h1>
      <p className="hint hint-top">
        Patients find you by your specializations and book appointments in your working hours.
      </p>
      <DoctorForm submitLabel="Register" submittingLabel="Registering…" onSubmit={register}
                  onCancel={() => navigate('/profile')} />
    </section>
  );
}
