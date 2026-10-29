// Calendar arithmetic in the browser's time zone. Weeks start on Monday.

export const DAYS_OF_WEEK = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

export function startOfDay(date) {
  const day = new Date(date);
  day.setHours(0, 0, 0, 0);
  return day;
}

export function addDays(date, days) {
  const moved = new Date(date);
  moved.setDate(moved.getDate() + days);
  return moved;
}

export function startOfWeek(date) {
  const day = startOfDay(date);
  return addDays(day, -((day.getDay() + 6) % 7));
}

export function startOfMonth(date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

export function addMonths(date, months) {
  return new Date(date.getFullYear(), date.getMonth() + months, 1);
}

export function isSameDay(a, b) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

/** MONDAY to SUNDAY, the names the API uses for days of the week. */
export function dayOfWeek(date) {
  return DAYS_OF_WEEK[(date.getDay() + 6) % 7];
}

/** YYYY-MM-DD, the format of date inputs and of the dates in page URLs. */
export function toDateKey(date) {
  return date.toLocaleDateString('en-CA');
}

/** Parses YYYY-MM-DD as local midnight, so the day does not shift with the time zone. */
export function fromDateKey(key) {
  const date = key ? new Date(`${key}T00:00:00`) : new Date(NaN);
  return Number.isNaN(date.getTime()) ? null : date;
}

export function formatTime(date) {
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

export function formatTimeRange(start, end) {
  return `${formatTime(start)} – ${formatTime(end)}`;
}

/** Such as "Tuesday, 28 October 2026", in the browser's language. */
export function formatLongDate(date) {
  return date.toLocaleDateString([], { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
}

/** Such as "Monday" for MONDAY. */
export function formatDayOfWeek(day) {
  return day.charAt(0) + day.slice(1).toLowerCase();
}

/** The browser's time zone, such as Asia/Almaty. */
export const browserTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;

/** Such as "27 Oct – 2 Nov 2026" for the week that starts on the date. */
export function formatWeekRange(weekStart) {
  const weekEnd = addDays(weekStart, 6);
  return `${weekStart.toLocaleDateString([], { day: 'numeric', month: 'short' })} – ${
    weekEnd.toLocaleDateString([], { day: 'numeric', month: 'short', year: 'numeric' })}`;
}
