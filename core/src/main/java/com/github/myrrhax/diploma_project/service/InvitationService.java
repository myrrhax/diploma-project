package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.event.SendMailEvent;
import com.github.myrrhax.diploma_project.model.entity.InvitationEntity;
import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import com.github.myrrhax.diploma_project.model.enums.AuthorityType;
import com.github.myrrhax.diploma_project.model.exception.SchemaNotFoundException;
import com.github.myrrhax.diploma_project.repository.InvitationRepository;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class InvitationService {
    private final InvitationRepository invitationRepository;
    private final SchemeRepository schemeRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;

    public void sendInvitation(UUID sender, UUID schemeId, String email, List<AuthorityType> authorities) {
        SchemeEntity scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new SchemaNotFoundException(schemeId));
        UserEntity initiator = userRepository.findById(sender).get();
        publisher.publishEvent(new SendMailEvent(this,
                scheme,
                initiator,
                email,
                buildAuthorities(authorities)));
    }

    public void saveInvitation(InvitationEntity invitationEntity) {
        invitationRepository.save(invitationEntity);
    }

    private String[] buildAuthorities(List<AuthorityType> authorities) {
        return (String[]) authorities.stream()
                .map(AuthorityType::name)
                .toArray();
    }
}
