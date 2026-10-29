import { useEffect, useState } from 'react';
import { appointmentApi } from '../api/appointmentApi';
import { otherParty, roleClass } from '../utils/appointments';
import { addDays, formatTimeRange, fromDateKey } from '../utils/dates';
import ErrorMessage from './ErrorMessage';

const SORTS = [
  ['createdAt,desc', 'Newest bookings first'],
  ['createdAt,asc', 'Oldest bookings first'],
  ['startTime,asc', 'Earliest appointments first'],
  ['startTime,desc', 'Latest appointments first'],
];
const PAGE_SIZE = 10;
const SEARCH_DELAY_MS = 300;

/**
 * The user's appointments as a list that loads a page at a time, with search by specialization, sorting
 * and a date range. version changes when an appointment was changed elsewhere, to load the list again.
 */
export default function AppointmentList({ userId, version, onSelect }) {
  const [search, setSearch] = useState('');
  const [specialization, setSpecialization] = useState('');
  const [sort, setSort] = useState(SORTS[0][0]);
  const [fromKey, setFromKey] = useState('');
  const [toKey, setToKey] = useState('');
  const [items, setItems] = useState([]);
  const [nextCursor, setNextCursor] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Searches once the user stops typing, not on every key.
  useEffect(() => {
    const timer = setTimeout(() => setSpecialization(search.trim()), SEARCH_DELAY_MS);
    return () => clearTimeout(timer);
  }, [search]);

  // The "to" date counts as a whole day: the range ends at the start of the next day.
  const filters = {
    specialization,
    sort,
    size: PAGE_SIZE,
    from: fromDateKey(fromKey) ?? undefined,
    to: fromDateKey(toKey) ? addDays(fromDateKey(toKey), 1) : undefined,
  };

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError(null);
    appointmentApi.getAppointments(filters)
      .then((page) => {
        if (!current) return;
        setItems(page.items);
        setNextCursor(page.nextCursor);
      })
      .catch((e) => current && setError(e))
      .finally(() => current && setLoading(false));
    return () => {
      current = false;
    };
    // filters is rebuilt on every render from these values.
  }, [specialization, sort, fromKey, toKey, version]);

  async function loadMore() {
    setLoading(true);
    try {
      const page = await appointmentApi.getAppointments({ ...filters, cursor: nextCursor });
      setItems([...items, ...page.items]);
      setNextCursor(page.nextCursor);
    } catch (e) {
      setError(e);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="appointment-list">
      <div className="filters">
        <label className="field">
          <span className="field-label">Specialization</span>
          <input className="field-input" type="search" placeholder="Search, such as cardio" value={search}
                 onChange={(event) => setSearch(event.target.value)} />
        </label>
        <label className="field">
          <span className="field-label">Sort</span>
          <select className="field-input" value={sort} onChange={(event) => setSort(event.target.value)}>
            {SORTS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </label>
        <label className="field">
          <span className="field-label">From</span>
          <input className="field-input" type="date" value={fromKey} max={toKey || undefined}
                 onChange={(event) => setFromKey(event.target.value)} />
        </label>
        <label className="field">
          <span className="field-label">To</span>
          <input className="field-input" type="date" value={toKey} min={fromKey || undefined}
                 onChange={(event) => setToKey(event.target.value)} />
        </label>
      </div>

      <ErrorMessage error={error} />
      {!loading && !error && items.length === 0 && <p className="empty-state">No appointments match.</p>}
      <ul className="appointment-rows">
        {items.map((appointment) => {
          const start = new Date(appointment.startTime);
          return (
            <li key={appointment.id}>
              <button type="button" className={`appointment-row ${roleClass(appointment, userId)}`}
                      onClick={() => onSelect(appointment)}>
                <span className="appointment-date">
                  <span className="appointment-day">{start.getDate()}</span>
                  <span className="appointment-month">{start.toLocaleDateString([], { month: 'short' })}</span>
                </span>
                <span className="appointment-main">
                  <span className="appointment-who">{otherParty(appointment, userId)}</span>
                  <span className="appointment-when">
                    {start.toLocaleDateString([], { weekday: 'long' })},{' '}
                    {formatTimeRange(start, new Date(appointment.endTime))}
                  </span>
                </span>
                <span className="chip">{appointment.specializationName}</span>
              </button>
            </li>
          );
        })}
      </ul>
      {loading && <p className="hint">Loading appointments…</p>}
      {nextCursor && !loading && (
        <button type="button" className="button button-secondary button-block" onClick={loadMore}>Load more</button>
      )}
    </div>
  );
}
