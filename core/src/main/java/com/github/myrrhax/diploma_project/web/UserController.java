package com.github.myrrhax.diploma_project.web;

import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.service.AuthorityService;
import com.github.myrrhax.diploma_project.service.UserService;
import com.github.myrrhax.diploma_project.web.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/whoami")
    public ResponseEntity<UserDTO> whoami(@AuthenticationPrincipal TokenUser tokenUser) {
        return ResponseEntity.ok(
                userService.getUserById(tokenUser.getToken().userId())
        );
    }
}
