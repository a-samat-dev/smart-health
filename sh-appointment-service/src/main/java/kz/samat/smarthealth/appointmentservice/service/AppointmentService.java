package kz.samat.smarthealth.appointmentservice.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import kz.samat.smarthealth.appointmentservice.config.AppointmentProperties;
import kz.samat.smarthealth.appointmentservice.exception.AppointmentNotFoundException;
import kz.samat.smarthealth.appointmentservice.exception.BadRequestException;
import kz.samat.smarthealth.appointmentservice.exception.ForbiddenException;
import kz.samat.smarthealth.appointmentservice.exception.TimeConflictException;
import kz.samat.smarthealth.appointmentservice.model.dto.AppointmentFilter;
import kz.samat.smarthealth.appointmentservice.model.dto.AppointmentPageResponse;
import kz.samat.smarthealth.appointmentservice.model.dto.AppointmentResponse;
import kz.samat.smarthealth.appointmentservice.model.dto.CreateAppointmentRequest;
import kz.samat.smarthealth.appointmentservice.model.dto.DoctorDto;
import kz.samat.smarthealth.appointmentservice.model.dto.UpdateAppointmentRequest;
import kz.samat.smarthealth.appointmentservice.model.entity.Appointment;
import kz.samat.smarthealth.appointmentservice.repository.AppointmentRepository;
import kz.samat.smarthealth.appointmentservice.util.CursorUtils;
import kz.samat.smarthealth.appointmentservice.util.CursorUtils.Cursor;
import kz.samat.smarthealth.appointmentservice.util.SlotUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static kz.samat.smarthealth.appointmentservice.repository.AppointmentSpecifications.after;
import static kz.samat.smarthealth.appointmentservice.repository.AppointmentSpecifications.hasParticipant;
import static kz.samat.smarthealth.appointmentservice.repository.AppointmentSpecifications.specializationContains;
import static kz.samat.smarthealth.appointmentservice.repository.AppointmentSpecifications.startsAtOrAfter;
import static kz.samat.smarthealth.appointmentservice.repository.AppointmentSpecifications.startsBefore;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    /** The properties the list can be sorted by, and how to read them for a cursor. */
    private static final Map<String, Function<Appointment, Instant>> SORT_PROPERTIES = Map.of(
            "createdAt", Appointment::getCreatedAt,
            "startTime", Appointment::getStartTime);

    private final AppointmentRepository appointmentRepository;
    private final DoctorClient doctorClient;
    private final UserClient userClient;
    private final AppointmentProperties properties;
    private final ModelMapper modelMapper;

    /** Books one of the doctor's free slots for the client. */
    @Transactional
    public AppointmentResponse createAppointment(UUID clientId, CreateAppointmentRequest request) {
        if (request.getDoctorId().equals(clientId)) {
            throw new BadRequestException("You cannot book an appointment with yourself");
        }
        DoctorDto doctor = doctorClient.getDoctor(request.getDoctorId());
        DoctorDto.Specialization specialization = doctor.specialization(request.getSpecializationId())
                .orElseThrow(() -> new BadRequestException(
                        "The doctor does not offer specialization " + request.getSpecializationId()));
        Instant endTime = checkSlot(doctor, request.getStartTime());
        checkTimeIsFree(doctor.userId(), clientId, request.getStartTime(), endTime, null);

        Appointment appointment = modelMapper.map(request, Appointment.class);
        appointment.setEndTime(endTime);
        appointment.setDoctorName(doctor.fullName());
        appointment.setClientId(clientId);
        appointment.setClientName(userClient.getUser(clientId).fullName());
        appointment.setSpecializationName(specialization.name());
        // Flushing sets createdAt and updatedAt before the response is built.
        return toResponse(appointmentRepository.saveAndFlush(appointment));
    }

    /**
     * One page of the user's appointments, as patient and as doctor. Pages are cut with a cursor rather than
     * an offset, so appointments booked meanwhile do not shift the pages or repeat items.
     */
    @Transactional(readOnly = true)
    public AppointmentPageResponse getAppointments(UUID userId, AppointmentFilter filter) {
        String[] sortParts = filter.getSort().split(",");
        String property = sortParts[0];
        Sort.Direction direction = Sort.Direction.fromString(sortParts[1]);

        List<Specification<Appointment>> conditions = new ArrayList<>();
        conditions.add(hasParticipant(userId));
        if (StringUtils.hasText(filter.getSpecialization())) {
            conditions.add(specializationContains(filter.getSpecialization().trim()));
        }
        if (filter.getFrom() != null) {
            conditions.add(startsAtOrAfter(filter.getFrom()));
        }
        if (filter.getTo() != null) {
            conditions.add(startsBefore(filter.getTo()));
        }
        if (filter.getCursor() != null) {
            Cursor cursor = CursorUtils.decode(filter.getCursor());
            if (!cursor.sort().equals(filter.getSort())) {
                throw new BadRequestException("The cursor belongs to another sort order");
            }
            conditions.add(after(property, direction, cursor.value(), cursor.id()));
        }

        // The id breaks ties, so every appointment has one place in the order and the cursor can point past it.
        Sort sort = Sort.by(direction, property, "id");
        // One more than a page tells whether there is a next page.
        List<Appointment> found = appointmentRepository.findBy(Specification.allOf(conditions),
                query -> query.sortBy(sort).limit(filter.getSize() + 1).all());
        boolean hasNextPage = found.size() > filter.getSize();
        List<Appointment> page = hasNextPage ? found.subList(0, filter.getSize()) : found;

        String nextCursor = null;
        if (hasNextPage) {
            Appointment last = page.getLast();
            nextCursor = CursorUtils.encode(
                    new Cursor(filter.getSort(), SORT_PROPERTIES.get(property).apply(last), last.getId()));
        }
        return new AppointmentPageResponse(page.stream().map(this::toResponse).toList(), nextCursor);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(UUID id, UUID userId) {
        return toResponse(findOwnAppointment(id, userId));
    }

    /** Moves the appointment to another free slot of the same doctor. Its patient or doctor may do this. */
    @Transactional
    public AppointmentResponse updateAppointment(UUID id, UUID userId, UpdateAppointmentRequest request) {
        Appointment appointment = findOwnAppointment(id, userId);
        DoctorDto doctor = doctorClient.getDoctor(appointment.getDoctorId());
        Instant endTime = checkSlot(doctor, request.getStartTime());
        checkTimeIsFree(appointment.getDoctorId(), appointment.getClientId(), request.getStartTime(), endTime,
                appointment.getId());

        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(endTime);
        appointment.setNotes(request.getNotes());
        // Flushing sets updatedAt before the response is built.
        return toResponse(appointmentRepository.saveAndFlush(appointment));
    }

    /** Cancels the appointment, which frees its slot. Its patient or doctor may do this. */
    @Transactional
    public void deleteAppointment(UUID id, UUID userId) {
        appointmentRepository.delete(findOwnAppointment(id, userId));
    }

    /** Returns when an appointment starting at the time would end, if the time is one of the doctor's slots. */
    private Instant checkSlot(DoctorDto doctor, Instant startTime) {
        if (!startTime.isAfter(Instant.now())) {
            throw new BadRequestException("The appointment must start in the future");
        }
        if (!SlotUtils.isSlotStart(doctor, startTime, properties.slotDuration())) {
            throw new BadRequestException("The doctor has no slot starting at " + startTime);
        }
        return startTime.plus(properties.slotDuration());
    }

    /** Neither the doctor nor the patient may have another appointment then, in either role. */
    private void checkTimeIsFree(UUID doctorId, UUID clientId, Instant start, Instant end, UUID ignoredAppointmentId) {
        List<Appointment> overlapping = appointmentRepository.findOverlapping(List.of(doctorId, clientId), start, end)
                .stream()
                .filter(appointment -> !appointment.getId().equals(ignoredAppointmentId))
                .toList();
        if (overlapping.stream().anyMatch(appointment -> appointment.hasParticipant(doctorId))) {
            throw new TimeConflictException("This slot is already taken");
        }
        if (!overlapping.isEmpty()) {
            throw new TimeConflictException("The patient already has an appointment at this time");
        }
    }

    /** Only an appointment's patient and doctor can see and change it. */
    private Appointment findOwnAppointment(UUID id, UUID userId) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        if (!appointment.hasParticipant(userId)) {
            throw new ForbiddenException("You can only access your own appointments");
        }
        return appointment;
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return modelMapper.map(appointment, AppointmentResponse.class);
    }
}
