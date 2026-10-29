import { useEffect, useState } from 'react';
import { Link, useLocation, useSearchParams } from 'react-router';
import { appointmentApi } from '../api/appointmentApi';
import { tokenStorage } from '../api/tokenStorage';
import AppointmentDialog from '../components/AppointmentDialog';
import AppointmentList from '../components/AppointmentList';
import ErrorMessage from '../components/ErrorMessage';
import MonthGrid from '../components/MonthGrid';
import TimeGrid from '../components/TimeGrid';
import { otherParty, roleClass } from '../utils/appointments';
import {
  addDays, addMonths, formatLongDate, formatTime, formatTimeRange, formatWeekRange, fromDateKey, startOfDay,
  startOfMonth, startOfWeek, toDateKey,
} from '../utils/dates';

const VIEWS = [['day', 'Day'], ['week', 'Week'], ['month', 'Month'], ['list', 'List']];

/** The first day a calendar view shows, how many days it shows, and how far its arrows move. */
const CALENDARS = {
  day: { start: startOfDay, days: 1, move: (date, step) => addDays(date, step) },
  week: { start: startOfWeek, days: 7, move: (date, step) => addDays(date, 7 * step) },
  month: { start: (date) => startOfWeek(startOfMonth(date)), days: 42, move: (date, step) => addMonths(date, step) },
};

function calendarTitle(view, date) {
  if (view === 'day') return formatLongDate(date);
  if (view === 'week') return formatWeekRange(startOfWeek(date));
  return date.toLocaleDateString([], { month: 'long', year: 'numeric' });
}

// The view and the date it shows live in the URL, so reloading or sharing the page keeps them.
export default function AppointmentsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const view = VIEWS.some(([name]) => name === searchParams.get('view')) ? searchParams.get('view') : 'week';
  const date = fromDateKey(searchParams.get('date')) ?? startOfDay(new Date());
  const location = useLocation();
  const userId = tokenStorage.getUserId();

  const [message, setMessage] = useState(location.state?.message);
  const [appointments, setAppointments] = useState(null);
  const [error, setError] = useState(null);
  const [selected, setSelected] = useState(null);
  const [version, setVersion] = useState(0);

  const calendar = CALENDARS[view];
  const rangeStart = calendar?.start(date);

  useEffect(() => {
    if (!calendar) return undefined;
    let current = true;
    setAppointments(null);
    setError(null);
    appointmentApi.getAllAppointments(rangeStart, addDays(rangeStart, calendar.days))
      .then((loaded) => current && setAppointments(loaded))
      .catch((e) => current && setError(e));
    return () => {
      current = false;
    };
    // rangeStart is a new Date on every render; its time is what matters.
  }, [view, rangeStart?.getTime(), version]);

  const show = (params) => setSearchParams({ view, date: toDateKey(date), ...params });

  const items = (appointments ?? []).map((appointment) => {
    const start = new Date(appointment.startTime);
    const end = new Date(appointment.endTime);
    return {
      key: appointment.id,
      start,
      end,
      title: `${formatTimeRange(start, end)} · ${otherParty(appointment, userId)} · ${appointment.specializationName}`,
      label: (
        <>
          <span className="item-time">{formatTime(start)}</span>{' '}
          <span className="item-who">{otherParty(appointment, userId)}</span>
          {view !== 'month' && <span className="item-detail">{appointment.specializationName}</span>}
        </>
      ),
      className: `appointment ${roleClass(appointment, userId)}`,
      onClick: () => setSelected(appointment),
    };
  });
  const hasPatients = appointments?.some((appointment) => appointment.doctorId === userId);

  return (
    <section className="card card-wide">
      <div className="calendar-toolbar">
        <h1>Appointments</h1>
        <div className="segmented" role="group" aria-label="View">
          {VIEWS.map(([name, label]) => (
            <button key={name} type="button" aria-pressed={view === name} onClick={() => show({ view: name })}>
              {label}
            </button>
          ))}
        </div>
        <Link to="/doctors" className="button">Book an appointment</Link>
      </div>

      {message && (
        <div className="notice" role="status">
          <p>{message}</p>
          <button type="button" className="icon-button" aria-label="Dismiss" onClick={() => setMessage(null)}>×</button>
        </div>
      )}

      {view === 'list' ? (
        <AppointmentList userId={userId} version={version} onSelect={setSelected} />
      ) : (
        <>
          <div className="calendar-nav">
            <button type="button" className="button button-secondary" onClick={() => show({ date: toDateKey(new Date()) })}>
              Today
            </button>
            <div className="icon-buttons">
              <button type="button" className="icon-button" aria-label={`Previous ${view}`}
                      onClick={() => show({ date: toDateKey(calendar.move(date, -1)) })}>‹</button>
              <button type="button" className="icon-button" aria-label={`Next ${view}`}
                      onClick={() => show({ date: toDateKey(calendar.move(date, 1)) })}>›</button>
            </div>
            <h2 className="calendar-title">{calendarTitle(view, date)}</h2>
            {hasPatients && (
              <div className="legend">
                <span className="legend-item legend-patient">You booked</span>
                <span className="legend-item legend-doctor">Your patients</span>
              </div>
            )}
          </div>
          <ErrorMessage error={error} />
          {view === 'month' ? (
            <MonthGrid month={startOfMonth(date)} items={items}
                       onSelectDay={(day) => show({ view: 'day', date: toDateKey(day) })} />
          ) : (
            <TimeGrid days={Array.from({ length: calendar.days }, (_, index) => addDays(rangeStart, index))}
                      items={items} />
          )}
          {appointments?.length === 0 && (
            <p className="empty-state">
              No appointments {view === 'day' ? 'on this day' : `this ${view}`}.{' '}
              <Link to="/doctors">Find a doctor</Link> to book one.
            </p>
          )}
          {!appointments && !error && <p className="hint">Loading appointments…</p>}
        </>
      )}

      <AppointmentDialog appointment={selected} userId={userId} onClose={() => setSelected(null)}
                         onCancelled={() => {
                           setSelected(null);
                           setMessage('The appointment was cancelled.');
                           setVersion((current) => current + 1);
                         }} />
    </section>
  );
}
