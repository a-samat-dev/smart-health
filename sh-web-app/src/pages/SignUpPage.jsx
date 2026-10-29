import { useState } from 'react';
import { Link, NavLink } from 'react-router';
import { useAuth } from '../auth/AuthContext';
import ErrorMessage from '../components/ErrorMessage';
import FormField from '../components/FormField';

/**
 * Creates the account. Doctors sign up the same way and then register their license and practice, which is
 * where GuestOnly sends them after /sign-up/doctor.
 */
export default function SignUpPage({ asDoctor = false }) {
  const { signUp } = useAuth();
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '' });
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const update = (event) => setForm({ ...form, [event.target.name]: event.target.value });

  // Sign-up also signs the user in; GuestOnly then moves them on.
  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await signUp(form);
    } catch (e) {
      setError(e);
      setSubmitting(false);
    }
  }

  return (
    <section className="card card-narrow">
      <h1>Create an account</h1>
      <nav className="tabs" aria-label="Account type">
        <NavLink to="/sign-up" end className="tab">I am a patient</NavLink>
        <NavLink to="/sign-up/doctor" className="tab">I am a doctor</NavLink>
      </nav>
      {asDoctor && (
        <p className="hint hint-top">
          After creating your account you add your license, specializations and working hours.
        </p>
      )}
      <form className="form" onSubmit={handleSubmit}>
        <div className="form-row">
          <FormField label="First name" name="firstName" autoComplete="given-name" required maxLength={100}
                     value={form.firstName} onChange={update} />
          <FormField label="Last name" name="lastName" autoComplete="family-name" required maxLength={100}
                     value={form.lastName} onChange={update} />
        </div>
        <FormField label="Email" name="email" type="email" autoComplete="email" required
                   value={form.email} onChange={update} />
        <FormField label="Password (at least 8 characters)" name="password" type="password"
                   autoComplete="new-password" required minLength={8} maxLength={72}
                   value={form.password} onChange={update} />
        <ErrorMessage error={error} />
        <button type="submit" className="button" disabled={submitting}>
          {submitting ? 'Creating account…' : asDoctor ? 'Continue' : 'Sign up'}
        </button>
      </form>
      <p className="hint">Already have an account? <Link to="/sign-in">Sign in</Link></p>
    </section>
  );
}
