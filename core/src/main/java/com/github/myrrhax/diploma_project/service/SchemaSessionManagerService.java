package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.model.dto.ConnectionChangedPayload;
import com.github.myrrhax.diploma_project.model.dto.UserDTO;
import com.github.myrrhax.diploma_project.security.TokenUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchemaSessionManagerService {
    boolean tryAddUser(UUID schemaId, String sessionId, UUID userId);
    boolean tryRemoveUser(String sessionId);
    void removeByUserId(UUID userId);
    boolean isConnected(String sessionId, UUID schemaId);
    List<UserDTO> getConnectedUsers(UUID schemaId);
    Optional<UserDTO> getUserBySessionId(String sessionId);
    Optional<UUID> getSchemaIdBySessionId(String sessionId);
}
