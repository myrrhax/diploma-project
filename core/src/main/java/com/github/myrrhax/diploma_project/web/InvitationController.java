package com.github.myrrhax.diploma_project.web;

import com.github.myrrhax.diploma_project.model.dto.InviteUserDTO;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.service.InvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invitations")
public class InvitationController {
    private final InvitationService invitationService;

    @PostMapping
    @PreAuthorize("@authorityService.hasAuthority(#tokenUser.token.userId, #dto.schemeId, 'INVITE_USERS')")
    public ResponseEntity<Void> inviteUser(@RequestBody @Validated InviteUserDTO dto,
                                           @AuthenticationPrincipal TokenUser tokenUser) {
        invitationService.sendInvitation(tokenUser.getToken().userId(),
                dto.schemeId(),
                dto.email(),
                dto.authorities());

        return ResponseEntity.ok().build();
    }
}
