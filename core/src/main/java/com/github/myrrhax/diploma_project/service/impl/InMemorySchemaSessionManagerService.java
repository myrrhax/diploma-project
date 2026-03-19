package com.github.myrrhax.diploma_project.service.impl;

import com.github.myrrhax.diploma_project.model.dto.UserDTO;
import com.github.myrrhax.diploma_project.service.SchemaSessionManagerService;
import com.github.myrrhax.diploma_project.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class InMemorySchemaSessionManagerService implements SchemaSessionManagerService {
    private final UserService userService;

    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, UserDTO>> schemaUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> sessionToSchema = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UserDTO> userMapping = new ConcurrentHashMap<>();

    @Value("${app.schemas.max-sessions:5}")
    private int maxSchemaSessions;

    @Override
    @Transactional(readOnly = true)
    public boolean tryAddUser(UUID schemaId, String sessionId, UUID userId) {
        AtomicBoolean isAdded = new AtomicBoolean(false);
        UserDTO user = userService.getUserById(userId);

        schemaUsers.compute(schemaId, (id, users) -> {
           if (users == null) {
               users = new ConcurrentHashMap<>();
           }

           if (users.size() >= maxSchemaSessions && !users.containsKey(sessionId)) {
               isAdded.set(false);
               return users;
           }

           users.put(sessionId, user);
           isAdded.set(true);
           return users;
        });

        if (isAdded.get()) {
            sessionToSchema.put(sessionId, schemaId);
            userMapping.put(sessionId, user);
            return true;
        }

        return false;
    }

    @Override
    public boolean tryRemoveUser(String sessionId) {
        UUID boardId = sessionToSchema.remove(sessionId);
        if (boardId == null) {
            return false;
        }

        AtomicReference<UserDTO> removedUser = new AtomicReference<>();

        schemaUsers.computeIfPresent(boardId, (id, users) -> {
            UserDTO removed = users.remove(sessionId);
            removedUser.set(removed);

            if (users.isEmpty()) {
                return null;
            }
            return users;
        });

        UserDTO user = removedUser.get();
        if (user != null) {
            sessionToSchema.remove(sessionId);
            userMapping.remove(sessionId);
        }

        return user != null;
    }

    @Override
    public boolean isConnected(String sessionId, UUID schemaId) {
        return sessionToSchema.containsKey(sessionId) && sessionToSchema.get(sessionId).equals(schemaId);
    }

    @Override
    public List<UserDTO> getConnectedUsers(UUID schemaId) {
        ConcurrentHashMap<String, UserDTO> users = schemaUsers.get(schemaId);

        return users == null
                ? Collections.emptyList()
                : users.values().stream()
                    .distinct()
                    .toList();
    }

    @Override
    public Optional<UserDTO> getUserBySessionId(String sessionId) {
        return Optional.ofNullable(userMapping.get(sessionId));
    }

    @Override
    public Optional<UUID> getSchemaIdBySessionId(String sessionId) {
        return Optional.ofNullable(sessionToSchema.get(sessionId));
    }
}
