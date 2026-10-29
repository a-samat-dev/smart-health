package kz.samat.smarthealth.appointmentservice.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import kz.samat.smarthealth.appointmentservice.config.AppointmentProperties;
import kz.samat.smarthealth.appointmentservice.exception.BadRequestException;
import kz.samat.smarthealth.appointmentservice.model.dto.DoctorDto;
import kz.samat.smarthealth.appointmentservice.model.dto.SlotResponse;
import kz.samat.smarthealth.appointmentservice.model.entity.Appointment;
import kz.samat.smarthealth.appointmentservice.repository.AppointmentRepository;
import kz.samat.smarthealth.appointmentservice.util.SlotUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A doctor's schedule: their working hours cut into slots, and which slots are still free. */
@Service
@RequiredArgsConstructor
public class SlotService {

    private static final Duration MAX_PERIOD = Duration.ofDays(31);

    private final AppointmentRepository appointmentRepository;
    private final DoctorClient doctorClient;
    private final AppointmentProperties properties;

    /**
     * The doctor's slots that lie within the period and have not started yet. A slot is unavailable when the
     * doctor has an appointment then, including one they booked with another doctor as a patient.
     */
    @Transactional(readOnly = true)
    public List<SlotResponse> getSlots(UUID doctorId, Instant from, Instant to) {
        if (!from.isBefore(to) || Duration.between(from, to).compareTo(MAX_PERIOD) > 0) {
            throw new BadRequestException("from must be before to, and at most 31 days earlier");
        }
        DoctorDto doctor = doctorClient.getDoctor(doctorId);
        ZoneId zone = ZoneId.of(doctor.timeZone());
        Duration slotDuration = properties.slotDuration();
        List<Appointment> appointments = appointmentRepository.findOverlapping(List.of(doctorId), from, to);
        Instant now = Instant.now();

        List<SlotResponse> slots = new ArrayList<>();
        LocalDate lastDate = to.atZone(zone).toLocalDate();
        for (LocalDate date = from.atZone(zone).toLocalDate(); !date.isAfter(lastDate); date = date.plusDays(1)) {
            for (Instant start : SlotUtils.slotStarts(doctor, date, slotDuration)) {
                Instant end = start.plus(slotDuration);
                if (start.isAfter(now) && !start.isBefore(from) && !end.isAfter(to)) {
                    boolean taken = appointments.stream()
                            .anyMatch(appointment -> appointment.getStartTime().isBefore(end)
                                    && appointment.getEndTime().isAfter(start));
                    slots.add(new SlotResponse(start, end, !taken));
                }
            }
        }
        return slots;
    }
}
