import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { appointmentApi } from '../api/appointmentApi';
import { formatLongDate, formatTimeRange } from '../utils/dates';
import { otherParty } from '../utils/appointments';
import Dialog from './Dialog';
import ErrorMessage from './ErrorMessage';

/** An appointment's details, with rescheduling and cancelling while it is still ahead. */
export default function AppointmentDialog({ appointment, userId, onClose, onCancelled }) {
  const [confirming, setConfirming] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    setConfirming(false);
    setError(null);
  }, [appointment?.id]);

  async function cancel() {
    setCancelling(true);
    setError(null);
    try {
      await appointmentApi.deleteAppointment(appointment.id);
      onCancelled();
    } catch (e) {
      setError(e);
    } finally {
      setCancelling(false);
    }
  }

  const start = appointment && new Date(appointment.startTime);
  const isAhead = start > new Date();
  return (
    <Dialog open={Boolean(appointment)} title={appointment && otherParty(appointment, userId)} onClose={onClose}>
      {appointment && (
        <>
          <dl className="details">
            <dt>When</dt>
            <dd>{formatLongDate(start)}<br />{formatTimeRange(start, new Date(appointment.endTime))}</dd>
            <dt>Specialization</dt>
            <dd>{appointment.specializationName}</dd>
            <dt>{appointment.clientId === userId ? 'Doctor' : 'Patient'}</dt>
            <dd>{appointment.clientId === userId ? `Dr. ${appointment.doctorName}` : appointment.clientName}</dd>
            <dt>Notes</dt>
            <dd>{appointment.notes ?? <span className="empty">None</span>}</dd>
            <dt>Booked</dt>
            <dd>{new Date(appointment.createdAt).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })}</dd>
          </dl>
          <ErrorMessage error={error} />
          {!isAhead && <p className="hint">This appointment has already started.</p>}
          {isAhead && !confirming && (
            <div className="actions">
              <Link to={`/doctors/${appointment.doctorId}/book?appointment=${appointment.id}`} className="button">
                Reschedule
              </Link>
              <button type="button" className="button button-danger" onClick={() => setConfirming(true)}>
                Cancel appointment
              </button>
            </div>
          )}
          {isAhead && confirming && (
            <div className="confirm">
              <p>Cancel this appointment? Its slot becomes free for others.</p>
              <div className="actions">
                <button type="button" className="button button-danger" disabled={cancelling} onClick={cancel}>
                  {cancelling ? 'Cancelling…' : 'Yes, cancel it'}
                </button>
                <button type="button" className="button button-secondary" disabled={cancelling}
                        onClick={() => setConfirming(false)}>
                  Keep it
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </Dialog>
  );
}
