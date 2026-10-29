package kz.samat.smarthealth.doctorservice.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

/** One page of a list, numbered from 0. */
@Getter
@AllArgsConstructor
public class PageResponse<T> {

    private final List<T> items;

    private final int page;

    private final int size;

    private final long totalItems;

    private final int totalPages;

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }
}
