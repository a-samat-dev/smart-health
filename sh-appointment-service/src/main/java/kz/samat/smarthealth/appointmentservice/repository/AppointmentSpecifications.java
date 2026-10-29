package kz.samat.smarthealth.appointmentservice.repository;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.criteria.Path;
import kz.samat.smarthealth.appointmentservice.model.entity.Appointment;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/** The conditions the appointment list can be narrowed down with. */
public final class AppointmentSpecifications {

    private AppointmentSpecifications() {
    }

    /** Appointments the user has, as the patient or as the doctor. */
    public static Specification<Appointment> hasParticipant(UUID userId) {
        return (root, query, cb) -> cb.or(cb.equal(root.get("clientId"), userId), cb.equal(root.get("doctorId"), userId));
    }

    public static Specification<Appointment> specializationContains(String text) {
        String escaped = text.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return (root, query, cb) -> cb.like(cb.lower(root.get("specializationName")), "%" + escaped + "%", '\\');
    }

    public static Specification<Appointment> startsAtOrAfter(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startTime"), from);
    }

    public static Specification<Appointment> startsBefore(Instant to) {
        return (root, query, cb) -> cb.lessThan(root.get("startTime"), to);
    }

    /**
     * Appointments that come after the given one in a list sorted by the property and then by id: the keyset
     * condition behind cursor pagination.
     */
    public static Specification<Appointment> after(String property, Sort.Direction direction, Instant value, UUID id) {
        return (root, query, cb) -> {
            Path<Instant> sortValue = root.get(property);
            Path<UUID> sortId = root.get("id");
            return direction.isAscending()
                    ? cb.or(cb.greaterThan(sortValue, value), cb.and(cb.equal(sortValue, value), cb.greaterThan(sortId, id)))
                    : cb.or(cb.lessThan(sortValue, value), cb.and(cb.equal(sortValue, value), cb.lessThan(sortId, id)));
        };
    }
}
