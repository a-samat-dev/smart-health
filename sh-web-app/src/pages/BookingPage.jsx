import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router';
import { appointmentApi } from '../api/appointmentApi';
import { doctorApi } from '../api/doctorApi';
import { tokenStorage } from '../api/tokenStorage';
import ErrorMessage from '../components/ErrorMessage';
import TimeGrid from '../components/TimeGrid';
import {
  addDays, browserTimeZone, dayOfWeek, formatLongDate, formatTime, formatTimeRange, formatWeekRange, isSameDay,
  startOfWeek, toDateKey,
} from '../utils/dates';

/**
 * A doctor's week of slots, to book one. With ?appointment={id} it moves that appointment to another slot
 * instead.
 */
export default function BookingPage() {
  const { doctorId } = useParams();
  const [searchParams] = useSearchParams();
  const rescheduleId = searchParams.get('appointment');
  const navigate = useNavigate();
  const isOwnSchedule = doctorId === tokenStorage.getUserId();

  const [doctor, setDoctor] = useState(null);
  const [appointment, setAppointment] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()));
  const [slots, setSlots] = useState(null);
  const [slotsVersion, setSlotsVersion] = useState(0);
  const [selected, setSelected] = useState(null);
  const [specializationId, setSpecializationId] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const panelRef = useRef(null);

  useEffect(() => {
    let current = true;
    Promise.all([doctorApi.getDoctor(doctorId), rescheduleId ? appointmentApi.getAppointment(rescheduleId) : null])
      .then(([loadedDoctor, loadedAppointment]) => {
        if (!current) return;
        setDoctor(loadedDoctor);
        setSpecializationId(String(loadedDoctor.specializations[0]?.id ?? ''));
        if (loadedAppointment) {
          setAppointment(loadedAppointment);
          setNotes(loadedAppointment.notes ?? '');
          setWeekStart(startOfWeek(new Date(loadedAppointment.startTime)));
        }
      })
      .catch((e) => current && setLoadError(e));
    return () => {
      current = false;
    };
  }, [doctorId, rescheduleId]);

  // On narrow screens the panel is under the calendar, out of sight of the slot that was picked.
  useEffect(() => {
    if (selected) {
      panelRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }, [selected]);

  useEffect(() => {
    let current = true;
    setSlots(null);
    appointmentApi.getSlots(doctorId, weekStart, addDays(weekStart, 7))
      .then((loaded) => current && setSlots(loaded))
      .catch((e) => current && setError(e));
    return () => {
      current = false;
    };
  }, [doctorId, weekStart, slotsVersion]);

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const changes = { startTime: selected.startTime, notes: notes.trim() || null };
      const saved = appointment
        ? await appointmentApi.updateAppointment(appointment.id, changes)
        : await appointmentApi.createAppointment({ ...changes, doctorId, specializationId: Number(specializationId) });
      const start = new Date(saved.startTime);
      navigate(`/appointments?view=week&date=${toDateKey(start)}`, {
        state: {
          message: `${appointment ? 'Moved to' : 'Booked for'} ${formatLongDate(start)}, `
            + `${formatTimeRange(start, new Date(saved.endTime))}, with Dr. ${saved.doctorName}.`,
        },
      });
    } catch (e) {
      setError(e);
      setSaving(false);
      // Someone else booked the slot first: show the week as it is now.
      if (e.status === 409) {
        setSelected(null);
        setSlotsVersion((version) => version + 1);
      }
    }
  }

  if (loadError) {
    return <section className="card"><ErrorMessage error={loadError} /></section>;
  }
  if (!doctor) {
    return <section className="card"><p className="hint">Loading the doctor's schedule…</p></section>;
  }

  const days = Array.from({ length: 7 }, (_, index) => addDays(weekStart, index));
  const items = (slots ?? []).map((slot) => {
    const start = new Date(slot.startTime);
    const end = new Date(slot.endTime);
    const isCurrent = appointment?.startTime === slot.startTime;
    const isSelected = selected?.startTime === slot.startTime;
    const bookable = slot.available && !isOwnSchedule;
    let label = formatTime(start);
    if (isCurrent) label = 'Current';
    else if (!slot.available) label = 'Booked';
    return {
      key: slot.startTime,
      start,
      end,
      label,
      title: `${formatTimeRange(start, end)}${slot.available ? '' : ' · booked'}`,
      className: ['slot', isSelected && 'is-selected', isCurrent && 'is-current', !slot.available && 'is-taken']
        .filter(Boolean).join(' '),
      onClick: bookable ? () => setSelected(slot) : undefined,
    };
  });
  const dayNote = (day) => (doctor.daysOff.includes(dayOfWeek(day)) && !items.some((item) => isSameDay(item.start, day))
    ? 'Day off' : null);
  const isCurrentWeek = weekStart <= startOfWeek(new Date());
  const specialization = doctor.specializations.find((s) => String(s.id) === specializationId);

  return (
    <section className="card card-wide">
      <div className="card-header">
        <div>
          <h1>
            {isOwnSchedule ? 'Your schedule' : `${appointment ? 'Reschedule with' : 'Book'} Dr. ${doctor.firstName} ${doctor.lastName}`}
          </h1>
          <p className="hint hint-flush">
            {doctor.specializations.map((s) => s.name).join(', ')} · {doctor.address.street} {doctor.address.houseNumber},{' '}
            {doctor.address.city}
          </p>
        </div>
        <Link to={appointment ? '/appointments' : '/doctors'} className="button button-secondary">Back</Link>
      </div>

      <div className="calendar-nav">
        <button type="button" className="button button-secondary" onClick={() => setWeekStart(startOfWeek(new Date()))}>
          This week
        </button>
        <div className="icon-buttons">
          <button type="button" className="icon-button" aria-label="Previous week" disabled={isCurrentWeek}
                  onClick={() => setWeekStart(addDays(weekStart, -7))}>‹</button>
          <button type="button" className="icon-button" aria-label="Next week"
                  onClick={() => setWeekStart(addDays(weekStart, 7))}>›</button>
        </div>
        <h2 className="calendar-title">{formatWeekRange(weekStart)}</h2>
        <div className="legend">
          <span className="legend-item legend-free">Free</span>
          <span className="legend-item legend-taken">Booked</span>
        </div>
      </div>
      {doctor.timeZone !== browserTimeZone && (
        <p className="hint hint-flush">
          Times are in your time zone, {browserTimeZone}. The doctor works {doctor.workingHours.start} –{' '}
          {doctor.workingHours.end} in {doctor.timeZone}.
        </p>
      )}

      <div className="booking-layout">
        <div>
          <TimeGrid days={days} items={items} dayNote={dayNote} />
          {slots?.length === 0 && <p className="empty-state">No free slots this week. Try the next one.</p>}
          {!slots && <p className="hint">Loading slots…</p>}
        </div>

        <aside className="booking-panel" ref={panelRef}>
          {isOwnSchedule ? (
            <p className="hint hint-flush">This is your own schedule. Patients book these slots.</p>
          ) : !selected ? (
            <>
              <h2>Choose a time</h2>
              <p className="hint hint-flush">Pick a free slot in the calendar.</p>
              <ErrorMessage error={error} />
            </>
          ) : (
            <form className="form" onSubmit={handleSubmit}>
              <h2>{formatLongDate(new Date(selected.startTime))}</h2>
              <p className="booking-time">{formatTimeRange(new Date(selected.startTime), new Date(selected.endTime))}</p>
              {appointment ? (
                <p className="hint hint-flush">{appointment.specializationName}</p>
              ) : doctor.specializations.length > 1 ? (
                <label className="field">
                  <span className="field-label">Specialization</span>
                  <select className="field-input" value={specializationId}
                          onChange={(event) => setSpecializationId(event.target.value)}>
                    {doctor.specializations.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                  </select>
                </label>
              ) : (
                <p className="hint hint-flush">{specialization?.name}</p>
              )}
              <label className="field">
                <span className="field-label">Notes for the doctor (optional)</span>
                <textarea className="field-input" rows={4} maxLength={1000} value={notes}
                          placeholder="Symptoms, questions, anything the doctor should know"
                          onChange={(event) => setNotes(event.target.value)} />
              </label>
              <ErrorMessage error={error} />
              <div className="actions">
                <button type="submit" className="button" disabled={saving}>
                  {saving ? 'Saving…' : appointment ? 'Move appointment' : 'Book appointment'}
                </button>
                <button type="button" className="button button-secondary" disabled={saving}
                        onClick={() => setSelected(null)}>
                  Cancel
                </button>
              </div>
            </form>
          )}
        </aside>
      </div>
    </section>
  );
}
