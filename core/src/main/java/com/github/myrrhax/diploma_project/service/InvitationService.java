package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.event.SendMailEvent;
import com.github.myrrhax.diploma_project.model.entity.InvitationEntity;
import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import com.github.myrrhax.diploma_project.model.enums.AuthorityType;
import com.github.myrrhax.diploma_project.model.enums.MailType;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.model.exception.SchemaNotFoundException;
import com.github.myrrhax.diploma_project.model.payload.SchemeInvitationMailPayload;
import com.github.myrrhax.diploma_project.repository.InvitationRepository;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
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
        log.info("Sending invitation for user {} and scheme {} from user {}", email, schemeId, sender);
        SchemeEntity scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new SchemaNotFoundException(schemeId));
        if (schemeRepository.containsUserWithEmailInScheme(email, schemeId)) {
            throw new ApplicationException("User already participating in scheme " + schemeId, HttpStatus.BAD_REQUEST);
        }
        UserEntity initiator = userRepository.findById(sender).get();
        String[] parsedAuthorities = buildAuthorities(authorities);
        log.info("Applying authorities [{}]", String.join(",", parsedAuthorities));

        InvitationEntity invitation = InvitationEntity.builder()
                .scheme(scheme)
                .initiator(initiator)
                .authorities(parsedAuthorities)
                .build();
        invitationRepository.saveAndFlush(invitation);
        log.info("Invitation {} was saved in database", invitation.getId());

        publisher.publishEvent(new SendMailEvent<>(this,
                email,
                MailType.SCHEME_INVITATION,
                new SchemeInvitationMailPayload(
                        scheme.getName(),
                        initiator.getEmail(),
                        parsedAuthorities,
                        "")
        ));
    }

    private String[] buildAuthorities(List<AuthorityType> authorities) {
        return authorities.stream()
                .map(AuthorityType::name)
                .toArray(String[]::new);
    }

    private String buildInvitationUrl() {
        return "";
    }
}
