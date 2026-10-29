import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { userApi } from '../api/userApi';
import DoctorDetails from '../components/DoctorDetails';
import ErrorMessage from '../components/ErrorMessage';
import FormField from '../components/FormField';

// Today's local date as YYYY-MM-DD, the format date inputs use.
const today = new Date().toLocaleDateString('en-CA');

// Dates of birth have no time zone; parsing "1995-04-12" as local midnight keeps the day unchanged.
const formatDate = (isoDate) => new Date(`${isoDate}T00:00:00`).toLocaleDateString();

function toForm(profile) {
  return {
    firstName: profile.firstName,
    lastName: profile.lastName,
    phoneNumber: profile.phoneNumber ?? '',
    dateOfBirth: profile.dateOfBirth ?? '',
  };
}

export default function ProfilePage() {
  const [profile, setProfile] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const [form, setForm] = useState(null);
  const [saveError, setSaveError] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let current = true;
    userApi.getProfile()
      .then((loaded) => current && setProfile(loaded))
      .catch((error) => current && setLoadError(error));
    return () => {
      current = false;
    };
  }, []);

  const update = (event) => setForm({ ...form, [event.target.name]: event.target.value });

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setSaveError(null);
    try {
      // The API replaces every editable field, and an empty optional field is sent as null to clear it.
      const updated = await userApi.updateProfile({
        firstName: form.firstName,
        lastName: form.lastName,
        phoneNumber: form.phoneNumber.trim() || null,
        dateOfBirth: form.dateOfBirth || null,
      });
      setProfile(updated);
      setForm(null);
    } catch (error) {
      setSaveError(error);
    } finally {
      setSaving(false);
    }
  }

  if (loadError) {
    return <section className="card"><ErrorMessage error={loadError} /></section>;
  }
  if (!profile) {
    return <section className="card"><p className="hint">Loading your profile…</p></section>;
  }

  if (form) {
    return (
      <section className="card">
        <h1>Edit profile</h1>
        <form className="form" onSubmit={handleSubmit}>
          <div className="form-row">
            <FormField label="First name" name="firstName" required maxLength={100}
                       value={form.firstName} onChange={update} />
            <FormField label="Last name" name="lastName" required maxLength={100}
                       value={form.lastName} onChange={update} />
          </div>
          <FormField label="Phone number" name="phoneNumber" type="tel" autoComplete="tel" placeholder="+7 701 123 45 67"
                     value={form.phoneNumber} onChange={update} />
          <FormField label="Date of birth" name="dateOfBirth" type="date" max={today}
                     value={form.dateOfBirth} onChange={update} />
          <ErrorMessage error={saveError} />
          <div className="actions">
            <button type="submit" className="button" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
            <button type="button" className="button button-secondary" disabled={saving}
                    onClick={() => { setForm(null); setSaveError(null); }}>
              Cancel
            </button>
          </div>
        </form>
      </section>
    );
  }

  return (
    <div className="stack">
      <section className="card">
        <div className="card-header">
          <h1>{profile.firstName} {profile.lastName}</h1>
          <button type="button" className="button button-secondary" onClick={() => setForm(toForm(profile))}>
            Edit profile
          </button>
        </div>
        <dl className="details">
          <dt>Email</dt>
          <dd>{profile.email}</dd>
          <dt>First name</dt>
          <dd>{profile.firstName}</dd>
          <dt>Last name</dt>
          <dd>{profile.lastName}</dd>
          <dt>Phone number</dt>
          <dd>{profile.phoneNumber ?? <span className="empty">Not set</span>}</dd>
          <dt>Date of birth</dt>
          <dd>{profile.dateOfBirth ? formatDate(profile.dateOfBirth) : <span className="empty">Not set</span>}</dd>
          <dt>Account type</dt>
          <dd>{profile.type === 'DOCTOR' ? 'Doctor' : 'Patient'}</dd>
          <dt>Member since</dt>
          <dd>{new Date(profile.createdAt).toLocaleDateString()}</dd>
        </dl>
      </section>
      <section className="card">
        {profile.type === 'DOCTOR' ? <DoctorDetails userId={profile.id} /> : (
          <div className="card-header">
            <div>
              <h2>Are you a doctor?</h2>
              <p className="hint hint-flush">Add your license and working hours so patients can book appointments with you.</p>
            </div>
            <Link to="/doctor-registration" className="button">Register as a doctor</Link>
          </div>
        )}
      </section>
    </div>
  );
}
