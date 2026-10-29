package kz.samat.smarthealth.appointmentservice.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import kz.samat.smarthealth.appointmentservice.model.dto.DoctorDto;

/** A doctor's working hours cut into appointment slots. */
public final class SlotUtils {

    private SlotUtils() {
    }

    /**
     * The starts of the doctor's slots on a date in the doctor's time zone, from the start of the working hours
     * until the last slot that ends by their end. There are none on a day off.
     */
    public static List<Instant> slotStarts(DoctorDto doctor, LocalDate date, Duration slotDuration) {
        if (doctor.daysOff().contains(date.getDayOfWeek())) {
            return List.of();
        }
        ZoneId zone = ZoneId.of(doctor.timeZone());
        // Counted in seconds of the day, so the last slot of a day that ends at midnight cannot wrap around.
        long workEnd = doctor.workingHours().end().toSecondOfDay();
        long step = slotDuration.toSeconds();
        List<Instant> starts = new ArrayList<>();
        for (long second = doctor.workingHours().start().toSecondOfDay(); second + step <= workEnd; second += step) {
            starts.add(ZonedDateTime.of(date, LocalTime.ofSecondOfDay(second), zone).toInstant());
        }
        return starts;
    }

    /** Whether one of the doctor's slots starts at this moment. */
    public static boolean isSlotStart(DoctorDto doctor, Instant start, Duration slotDuration) {
        LocalDate date = start.atZone(ZoneId.of(doctor.timeZone())).toLocalDate();
        return slotStarts(doctor, date, slotDuration).contains(start);
    }
}
