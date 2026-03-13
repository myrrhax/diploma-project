package com.github.myrrhax.diploma_project.web;

import com.github.myrrhax.diploma_project.model.dto.GrantUserDTO;
import com.github.myrrhax.diploma_project.model.dto.InviteUserDTO;
import com.github.myrrhax.diploma_project.model.dto.ParticipationDto;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.service.AuthorityService;
import com.github.myrrhax.diploma_project.service.ParticipationService;
import com.github.myrrhax.shared.model.AuthorityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/participations")
public class ParticipationController {
    private final ParticipationService participationService;
    private final AuthorityService authorityService;

    @PostMapping("/invite")
    public ResponseEntity<Void> inviteUser(@RequestBody @Validated InviteUserDTO dto,
                                           @AuthenticationPrincipal TokenUser tokenUser) {
        participationService.sendInvitation(tokenUser.getToken().userId(),
                dto.schemeId(),
                dto.email(),
                dto.authorities());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm/{invitationId}")
    public ResponseEntity<ParticipationDto> confirmParticipation(@PathVariable("invitationId") UUID invitationId,
                                                                 @AuthenticationPrincipal TokenUser tokenUser) {
        return ResponseEntity.ok(
                participationService.confirmParticipation(tokenUser.getToken().userId(), invitationId)
        );
    }

    @GetMapping("/schema/{id}")
    public ResponseEntity<List<ParticipationDto>> getParticipants(@PathVariable UUID id) {
        return ResponseEntity.ok(
                participationService.getParticipants(id)
        );
    }

    @GetMapping("/my/{id}")
    public ResponseEntity<ParticipationDto> getMyParticipationInfo(@PathVariable UUID id,
                                                                   @AuthenticationPrincipal TokenUser user) {
        return ResponseEntity.ok(
                participationService.getParticipationInfo(id, user.getToken().userId())
        );
    }

    @PostMapping("/grant")
    public ResponseEntity<Void> grantUser(@RequestBody GrantUserDTO dto) {
        if (dto.authorities().contains(AuthorityType.ALL))
            throw new ApplicationException("Creator can't grant full access", HttpStatus.BAD_REQUEST);

        authorityService.grantUser(dto.userId(), dto.schemeId(), dto.authorities());
        return ResponseEntity.ok().build();
    }
}
