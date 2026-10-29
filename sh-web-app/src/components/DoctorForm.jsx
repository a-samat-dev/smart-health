import { useEffect, useState } from 'react';
import { doctorApi } from '../api/doctorApi';
import { browserTimeZone, DAYS_OF_WEEK, formatDayOfWeek } from '../utils/dates';
import ErrorMessage from './ErrorMessage';
import FormField from './FormField';

// Every time zone the browser knows, with the browser's own among them.
const TIME_ZONES = [...new Set([browserTimeZone, ...Intl.supportedValuesOf('timeZone')])].sort();

const EMPTY_FORM = {
  licenseNumber: '',
  specializationIds: [],
  country: '',
  city: '',
  street: '',
  houseNumber: '',
  contactEmail: '',
  contactPhone: '',
  workStart: '09:00',
  workEnd: '17:00',
  daysOff: ['SATURDAY', 'SUNDAY'],
  timeZone: browserTimeZone,
};

function toForm(doctor) {
  return {
    licenseNumber: doctor.licenseNumber,
    specializationIds: doctor.specializations.map((specialization) => specialization.id),
    ...doctor.address,
    contactEmail: doctor.contactEmail,
    contactPhone: doctor.contactPhone,
    workStart: doctor.workingHours.start,
    workEnd: doctor.workingHours.end,
    daysOff: doctor.daysOff,
    timeZone: doctor.timeZone,
  };
}

/** The profile in the shape the doctors API takes. */
function toProfile(form) {
  return {
    licenseNumber: form.licenseNumber.trim(),
    specializationIds: form.specializationIds,
    address: { country: form.country, city: form.city, street: form.street, houseNumber: form.houseNumber },
    contactEmail: form.contactEmail,
    contactPhone: form.contactPhone,
    workingHours: { start: form.workStart, end: form.workEnd },
    daysOff: form.daysOff,
    timeZone: form.timeZone,
  };
}

const toggle = (values, value) => (values.includes(value) ? values.filter((v) => v !== value) : [...values, value]);

/**
 * The doctor profile form, empty for registering or filled from doctor for editing. onSubmit(profile,
 * licenseFile) gets the profile in the API's shape and the chosen PDF, which may be null when editing.
 */
export default function DoctorForm({ doctor, submitLabel, submittingLabel, onSubmit, onCancel }) {
  const [form, setForm] = useState(() => (doctor ? toForm(doctor) : EMPTY_FORM));
  const [licenseFile, setLicenseFile] = useState(null);
  const [specializations, setSpecializations] = useState([]);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let current = true;
    doctorApi.getSpecializations()
      .then((loaded) => current && setSpecializations(loaded))
      .catch((loadError) => current && setError(loadError));
    return () => {
      current = false;
    };
  }, []);

  const update = (event) => setForm({ ...form, [event.target.name]: event.target.value });

  async function handleSubmit(event) {
    event.preventDefault();
    // The browser checks the other fields; these choices are buttons, so they are checked here.
    if (form.specializationIds.length === 0) {
      setError({ message: 'Choose at least one specialization.' });
      return;
    }
    if (form.daysOff.length === DAYS_OF_WEEK.length) {
      setError({ message: 'Leave at least one working day.' });
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await onSubmit(toProfile(form), licenseFile);
    } catch (e) {
      setError(e);
      setSubmitting(false);
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <fieldset className="fieldset">
        <legend>License</legend>
        <FormField label="License number" name="licenseNumber" required maxLength={50}
                   value={form.licenseNumber} onChange={update} />
        <label className="field">
          <span className="field-label">
            {doctor ? 'Replace the license file (PDF, optional)' : 'License file (PDF, at most 10 MB)'}
          </span>
          <input className="field-input" type="file" accept="application/pdf" required={!doctor}
                 onChange={(event) => setLicenseFile(event.target.files[0] ?? null)} />
        </label>
      </fieldset>

      <fieldset className="fieldset">
        <legend>Specializations</legend>
        <div className="chips">
          {specializations.map((specialization) => (
            <button key={specialization.id} type="button" className="chip"
                    aria-pressed={form.specializationIds.includes(specialization.id)}
                    onClick={() => setForm({ ...form, specializationIds: toggle(form.specializationIds, specialization.id) })}>
              {specialization.name}
            </button>
          ))}
        </div>
      </fieldset>

      <fieldset className="fieldset">
        <legend>Practice address</legend>
        <div className="form-row">
          <FormField label="Country" name="country" autoComplete="country-name" required maxLength={100}
                     value={form.country} onChange={update} />
          <FormField label="City" name="city" autoComplete="address-level2" required maxLength={100}
                     value={form.city} onChange={update} />
        </div>
        <div className="form-row form-row-wide-first">
          <FormField label="Street" name="street" autoComplete="address-line1" required maxLength={200}
                     value={form.street} onChange={update} />
          <FormField label="House number" name="houseNumber" required maxLength={20}
                     value={form.houseNumber} onChange={update} />
        </div>
        <div className="form-row">
          <FormField label="Contact email" name="contactEmail" type="email" required maxLength={255}
                     value={form.contactEmail} onChange={update} />
          <FormField label="Contact phone" name="contactPhone" type="tel" required placeholder="+7 727 123 45 67"
                     value={form.contactPhone} onChange={update} />
        </div>
      </fieldset>

      <fieldset className="fieldset">
        <legend>Working hours</legend>
        <div className="form-row">
          <FormField label="From" name="workStart" type="time" required value={form.workStart} onChange={update} />
          <FormField label="To" name="workEnd" type="time" required value={form.workEnd} onChange={update} />
        </div>
        <label className="field">
          <span className="field-label">Time zone</span>
          <select className="field-input" name="timeZone" value={form.timeZone} onChange={update}>
            {TIME_ZONES.map((zone) => <option key={zone} value={zone}>{zone}</option>)}
          </select>
        </label>
        <span className="field-label">Days off</span>
        <div className="chips">
          {DAYS_OF_WEEK.map((day) => (
            <button key={day} type="button" className="chip" aria-pressed={form.daysOff.includes(day)}
                    onClick={() => setForm({ ...form, daysOff: toggle(form.daysOff, day) })}>
              {formatDayOfWeek(day)}
            </button>
          ))}
        </div>
      </fieldset>

      <ErrorMessage error={error} />
      <div className="actions">
        <button type="submit" className="button" disabled={submitting}>{submitting ? submittingLabel : submitLabel}</button>
        {onCancel && (
          <button type="button" className="button button-secondary" disabled={submitting} onClick={onCancel}>
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
