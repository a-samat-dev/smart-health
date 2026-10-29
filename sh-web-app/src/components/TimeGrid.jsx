import { useEffect, useState } from 'react';
import { isSameDay } from '../utils/dates';

const DEFAULT_FIRST_HOUR = 8;
const DEFAULT_LAST_HOUR = 18;
const MINUTE = 60 * 1000;

const minutesOfDay = (date) => date.getHours() * 60 + date.getMinutes();

/** The hours to show: 08:00 to 18:00, widened to fit every item. */
function hourRange(items) {
  let first = DEFAULT_FIRST_HOUR;
  let last = DEFAULT_LAST_HOUR;
  items.forEach(({ start, end }) => {
    first = Math.min(first, start.getHours());
    // An item that ends at midnight ends at hour 24 of its own day.
    last = Math.max(last, isSameDay(start, end) ? Math.ceil(minutesOfDay(end) / 60) : 24);
  });
  return [first, last];
}

/** Ticks every minute, so the line that marks the current time moves. */
function useNow() {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), MINUTE);
    return () => clearInterval(timer);
  }, []);
  return now;
}

/**
 * Days side by side, with time running down, like a calendar's day and week views. Each item is placed by
 * its start and end Dates: { key, start, end, label, title, className, onClick, disabled }. dayNote(day)
 * may return a short text for a day, such as "Day off".
 */
export default function TimeGrid({ days, items, dayNote = () => null }) {
  const now = useNow();
  const [firstHour, lastHour] = hourRange(items);
  const hours = Array.from({ length: lastHour - firstHour }, (_, index) => firstHour + index);
  const totalMinutes = (lastHour - firstHour) * 60;
  const percentOfDay = (minutes) => `${((minutes - firstHour * 60) / totalMinutes) * 100}%`;

  return (
    <div className="time-grid-scroll">
      <div className="time-grid" style={{ '--days': days.length, '--hours': hours.length }}>
        <div className="time-grid-head">
          <div />
          {days.map((day) => {
            const note = dayNote(day);
            return (
              <div key={day.toISOString()} className={`time-grid-day${isSameDay(day, now) ? ' is-today' : ''}`}>
                <span className="time-grid-weekday">{day.toLocaleDateString([], { weekday: 'short' })}</span>
                <span className="time-grid-date">{day.getDate()}</span>
                {note && <span className="time-grid-note">{note}</span>}
              </div>
            );
          })}
        </div>
        <div className="time-grid-body">
          <div className="time-grid-gutter">
            {hours.map((hour) => (
              <span key={hour} className="time-grid-hour" style={{ top: percentOfDay(hour * 60) }}>
                {new Date(2000, 0, 1, hour).toLocaleTimeString([], { hour: 'numeric' })}
              </span>
            ))}
          </div>
          {days.map((day) => (
            <div key={day.toISOString()} className={`time-grid-column${dayNote(day) ? ' is-off' : ''}`}>
              {items.filter((item) => isSameDay(item.start, day)).map((item) => {
                const endMinutes = isSameDay(item.start, item.end) ? minutesOfDay(item.end) : 24 * 60;
                const style = {
                  top: percentOfDay(minutesOfDay(item.start)),
                  height: `${((endMinutes - minutesOfDay(item.start)) / totalMinutes) * 100}%`,
                };
                return (
                  <button key={item.key} type="button" className={`time-grid-item ${item.className ?? ''}`}
                          style={style} title={item.title} disabled={item.disabled || !item.onClick}
                          onClick={item.onClick}>
                    {item.label}
                  </button>
                );
              })}
              {isSameDay(day, now) && minutesOfDay(now) >= firstHour * 60 && minutesOfDay(now) < lastHour * 60 && (
                <div className="time-grid-now" style={{ top: percentOfDay(minutesOfDay(now)) }} aria-hidden="true" />
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
