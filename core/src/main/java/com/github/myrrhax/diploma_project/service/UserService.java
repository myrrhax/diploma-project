package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.mapper.UserMapper;
import com.github.myrrhax.diploma_project.model.dto.UserDTO;
import com.github.myrrhax.diploma_project.model.exception.UserNotFoundException;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    // ToDo добавить кэширование
    @Transactional(readOnly = true)
    public UserDTO getUserById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
