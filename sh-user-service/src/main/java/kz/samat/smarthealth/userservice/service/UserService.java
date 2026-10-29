package kz.samat.smarthealth.userservice.service;

import java.util.UUID;

import kz.samat.smarthealth.userservice.exception.EmailAlreadyExistsException;
import kz.samat.smarthealth.userservice.exception.ForbiddenException;
import kz.samat.smarthealth.userservice.exception.UserNotFoundException;
import kz.samat.smarthealth.userservice.model.dto.CreateUserRequest;
import kz.samat.smarthealth.userservice.model.dto.UpdateUserRequest;
import kz.samat.smarthealth.userservice.model.dto.UserResponse;
import kz.samat.smarthealth.userservice.model.entity.User;
import kz.samat.smarthealth.userservice.model.entity.UserType;
import kz.samat.smarthealth.userservice.repository.UserRepository;
import kz.samat.smarthealth.userservice.util.EmailUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    /** Signs the user up. */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String email = EmailUtils.normalize(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = modelMapper.map(request, User.class);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        // Flushing sets createdAt and updatedAt before the response is built.
        return modelMapper.map(userRepository.saveAndFlush(user), UserResponse.class);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id, UUID currentUserId) {
        return modelMapper.map(findOwnUser(id, currentUserId), UserResponse.class);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UUID currentUserId, UpdateUserRequest request) {
        User user = findOwnUser(id, currentUserId);
        modelMapper.map(request, user);
        // Flushing sets updatedAt before the response is built.
        return modelMapper.map(userRepository.saveAndFlush(user), UserResponse.class);
    }

    /** For other services: any user's profile, without the ownership check. */
    @Transactional(readOnly = true)
    public UserResponse getAnyUser(UUID id) {
        return modelMapper.map(findUser(id), UserResponse.class);
    }

    /** For sh-doctor-service, which makes a user a doctor once their doctor profile is saved. */
    @Transactional
    public UserResponse updateType(UUID id, UserType type) {
        User user = findUser(id);
        user.setType(type);
        return modelMapper.map(userRepository.saveAndFlush(user), UserResponse.class);
    }

    /** Deletes the user; the database removes their sessions with them. */
    @Transactional
    public void deleteUser(UUID id, UUID currentUserId) {
        userRepository.delete(findOwnUser(id, currentUserId));
    }

    /** Users can only see and change their own account. */
    private User findOwnUser(UUID id, UUID currentUserId) {
        if (!id.equals(currentUserId)) {
            throw new ForbiddenException("You can only access your own account");
        }
        return findUser(id);
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
