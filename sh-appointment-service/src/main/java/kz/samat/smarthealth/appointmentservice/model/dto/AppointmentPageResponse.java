package kz.samat.smarthealth.appointmentservice.model.dto;

import java.util.List;

/**
 * @param items      the appointments on this page
 * @param nextCursor pass as {@code cursor} to get the next page; null on the last page
 */
public record AppointmentPageResponse(List<AppointmentResponse> items, String nextCursor) {
}
