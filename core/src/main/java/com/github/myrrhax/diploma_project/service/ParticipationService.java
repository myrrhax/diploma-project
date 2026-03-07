package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.event.SendMailEvent;
import com.github.myrrhax.diploma_project.mapper.UserMapper;
import com.github.myrrhax.diploma_project.model.dto.ParticipationDto;
import com.github.myrrhax.diploma_project.model.dto.UserDTO;
import com.github.myrrhax.diploma_project.model.entity.AuthorityEntity;
import com.github.myrrhax.diploma_project.model.entity.InvitationEntity;
import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.model.exception.SchemaNotFoundException;
import com.github.myrrhax.diploma_project.repository.AuthorityRepository;
import com.github.myrrhax.diploma_project.repository.InvitationRepository;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import com.github.myrrhax.shared.model.AuthorityType;
import com.github.myrrhax.shared.model.MailType;
import com.github.myrrhax.shared.payload.SchemeInvitationMailPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ParticipationService {
    private final InvitationRepository invitationRepository;
    private final SchemeRepository schemeRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;
    private final AuthorityRepository authorityRepository;
    private final UserMapper userMapper;
    private final AuthorityService authorityService;
    private final UserService userService;

    @Value("${app.invitation.callback-url}")
    private String invitationCallbackUrlTemplate;

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
                .receiverEmail(email)
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
                        buildInvitationUrl(invitation))
        ));
    }

    // ToDo кэшировать
    @Transactional(readOnly = true)
    @PreAuthorize("@authorityCheckService.hasAccess(principal.token.userId, #schemaId)")
    public List<ParticipationDto> getParticipants(UUID schemaId) {
        List<AuthorityEntity> authorities = authorityRepository.findAllBySchemeId(schemaId);

        return authorities.stream()
                .collect(Collectors.groupingBy(
                        AuthorityEntity::getUser,
                        Collectors.mapping(AuthorityEntity::getType, Collectors.toList())
                ))
                .entrySet()
                .stream()
                .map(entry -> new ParticipationDto(
                        userMapper.toDto(entry.getKey()),
                        entry.getValue()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ParticipationDto getParticipationInfo(UUID schemaId, UUID userId) {
        UserDTO user = userService.getUserById(userId);
        Set<AuthorityType> userAuthorities = authorityService.getAuthorities(userId, schemaId);

        return new ParticipationDto(
                user,
                userAuthorities.stream().toList()
        );
    }

    @Transactional
    public ParticipationDto confirmParticipation(UUID userId, UUID invitationId) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        InvitationEntity invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ApplicationException("Invitation not found", HttpStatus.NOT_FOUND));

        if (!user.getEmail().equals(invitation.getReceiverEmail())) {
            throw new ApplicationException("Access denied for user with email " + user.getEmail(), HttpStatus.FORBIDDEN);
        }
        if (invitation.isConfirmed()) {
            throw new ApplicationException("Invitation is already confirmed");
        }

        invitation.setConfirmed(true);
        invitation.setConfirmedAt(LocalDateTime.now());

        List<AuthorityType> authorityTypes = Arrays.stream(invitation.getAuthorities())
                .map(AuthorityType::valueOf)
                .toList();
        List<AuthorityEntity> authorities = authorityTypes.stream()
                .map(it -> new AuthorityEntity(user, invitation.getScheme(), it))
                .toList();

        authorityRepository.saveAll(authorities);

        return new ParticipationDto(userMapper.toDto(user), authorityTypes);
    }

    private String[] buildAuthorities(List<AuthorityType> authorities) {
        return authorities.stream()
                .map(AuthorityType::name)
                .toArray(String[]::new);
    }

    private String buildInvitationUrl(InvitationEntity invitation) {
        return String.format(invitationCallbackUrlTemplate, invitation.getId());
    }
}
