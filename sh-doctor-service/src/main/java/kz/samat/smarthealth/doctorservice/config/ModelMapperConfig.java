package kz.samat.smarthealth.doctorservice.config;

import kz.samat.smarthealth.doctorservice.model.dto.DoctorRequest;
import kz.samat.smarthealth.doctorservice.model.entity.Doctor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        // Only identically named properties are copied.
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        // DoctorService copies the days off into the set Hibernate already tracks, rather than replacing it.
        modelMapper.typeMap(DoctorRequest.class, Doctor.class).addMappings(mapper -> mapper.skip(Doctor::setDaysOff));
        return modelMapper;
    }
}
