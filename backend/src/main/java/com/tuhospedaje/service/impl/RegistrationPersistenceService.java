package com.tuhospedaje.service.impl;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.EmailOutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class RegistrationPersistenceService {
    private final UserRepository userRepository;
    private final EmailOutboxService emailOutboxService;
    public RegistrationPersistenceService(UserRepository userRepository, EmailOutboxService emailOutboxService) {
        this.userRepository = userRepository;
        this.emailOutboxService = emailOutboxService;
    }
    @Transactional
    public User persist(RegisterRequest request, String encodedPassword) {
        User user = User.builder().firstName(request.getFirstName()).lastName(request.getLastName())
                .email(request.getEmail()).password(encodedPassword).role(RoleEnum.USER)
                .imageUrl("https://ui-avatars.com/api/?name=" + request.getFirstName() + "+" + request.getLastName())
                .build();
        userRepository.saveAndFlush(user);
        emailOutboxService.enqueueWelcome(user, request);
        return user;
    }
}
