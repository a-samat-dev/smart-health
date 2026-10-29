import { addDays, isSameDay, startOfWeek } from '../utils/dates';

const ITEMS_PER_DAY = 3;
const WEEKS = 6;

/**
 * A month as six weeks of days, like a calendar's month view, with up to three items a day. Items are
 * { key, start, label, className, onClick }; onSelectDay(day) opens a day.
 */
export default function MonthGrid({ month, items, onSelectDay }) {
  const firstDay = startOfWeek(month);
  const days = Array.from({ length: WEEKS * 7 }, (_, index) => addDays(firstDay, index));
  const today = new Date();

  return (
    <div className="month-grid">
      {days.slice(0, 7).map((day) => (
        <div key={`head-${day.getDay()}`} className="month-grid-weekday">
          {day.toLocaleDateString([], { weekday: 'short' })}
        </div>
      ))}
      {days.map((day) => {
        const dayItems = items.filter((item) => isSameDay(item.start, day));
        const classes = ['month-grid-day'];
        if (day.getMonth() !== month.getMonth()) classes.push('is-outside');
        if (isSameDay(day, today)) classes.push('is-today');
        return (
          <div key={day.toISOString()} className={classes.join(' ')}>
            <button type="button" className="month-grid-date" onClick={() => onSelectDay(day)}
                    aria-label={day.toLocaleDateString([], { dateStyle: 'full' })}>
              {day.getDate()}
            </button>
            {dayItems.slice(0, ITEMS_PER_DAY).map((item) => (
              <button key={item.key} type="button" className={`month-grid-item ${item.className ?? ''}`}
                      onClick={item.onClick}>
                {item.label}
              </button>
            ))}
            {dayItems.length > ITEMS_PER_DAY && (
              <button type="button" className="month-grid-more" onClick={() => onSelectDay(day)}>
                +{dayItems.length - ITEMS_PER_DAY} more
              </button>
            )}
          </div>
        );
      })}
    </div>
  );
}
