import { useState } from 'react';
import { Link } from 'react-router';
import { useAuth } from '../auth/AuthContext';
import ErrorMessage from '../components/ErrorMessage';
import FormField from '../components/FormField';

export default function SignInPage() {
  const { signIn } = useAuth();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const update = (event) => setForm({ ...form, [event.target.name]: event.target.value });

  // On success GuestOnly moves the user on, so this page only has to handle failures.
  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await signIn(form);
    } catch (e) {
      setError(e);
      setSubmitting(false);
    }
  }

  return (
    <section className="card card-narrow">
      <h1>Sign in</h1>
      <form className="form" onSubmit={handleSubmit}>
        <FormField label="Email" name="email" type="email" autoComplete="email" required
                   value={form.email} onChange={update} />
        <FormField label="Password" name="password" type="password" autoComplete="current-password" required
                   value={form.password} onChange={update} />
        <ErrorMessage error={error} />
        <button type="submit" className="button" disabled={submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p className="hint">No account yet? <Link to="/sign-up">Sign up</Link></p>
    </section>
  );
}
