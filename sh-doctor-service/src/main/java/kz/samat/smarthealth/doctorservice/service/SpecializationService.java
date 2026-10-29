package kz.samat.smarthealth.doctorservice.service;

import java.util.List;

import kz.samat.smarthealth.doctorservice.model.dto.SpecializationResponse;
import kz.samat.smarthealth.doctorservice.repository.SpecializationRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SpecializationService {

    private final SpecializationRepository specializationRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<SpecializationResponse> getSpecializations() {
        return specializationRepository.findAll(Sort.by("name")).stream()
                .map(specialization -> modelMapper.map(specialization, SpecializationResponse.class))
                .toList();
    }
}
