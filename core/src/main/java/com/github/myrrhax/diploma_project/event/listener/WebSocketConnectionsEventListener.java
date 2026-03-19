package com.github.myrrhax.diploma_project.event.listener;

import com.github.myrrhax.diploma_project.event.ServerEvent;
import com.github.myrrhax.diploma_project.model.dto.ConnectionChangedPayload;
import com.github.myrrhax.diploma_project.model.dto.UserDTO;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.service.SchemaSessionManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketConnectionsEventListener {
    private static final String TOPIC_PREFIX = "/topic/schema-connections/";
    private final SimpMessageSendingOperations messagingTemplate;
    private final SchemaSessionManagerService sessionManager;

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        PreAuthenticatedAuthenticationToken tokenAuth = (PreAuthenticatedAuthenticationToken) accessor.getUser();

        if (destination != null && destination.startsWith(TOPIC_PREFIX) && tokenAuth != null) {
            TokenUser tokenUser = (TokenUser) tokenAuth.getPrincipal();
            UserDTO user = sessionManager.getUserBySessionId(accessor.getSessionId()).orElseThrow(() -> {
                log.warn("User not found for sessionId {}", accessor.getSessionId());
                sessionManager.tryRemoveUser(sessionId);

                return new RuntimeException("Failed to connect user to schema");
            });
            String schemaId = destination.substring(TOPIC_PREFIX.length());
            UUID parsedSchemaId = UUID.fromString(schemaId);

            messagingTemplate.convertAndSend(destination,
                    new ServerEvent.ConnectionChangedEvent(new ConnectionChangedPayload(parsedSchemaId,
                            user,
                            ConnectionChangedPayload.ConnectionChangeType.CONNECTED)));
            List<UserDTO> currentUsers = sessionManager.getConnectedUsers(parsedSchemaId);

            SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
            headerAccessor.setSessionId(sessionId);
            headerAccessor.setLeaveMutable(true);

            messagingTemplate.convertAndSendToUser(
                    tokenUser.getUsername(),
                    "/queue/schema-connections/" + schemaId + "/users",
                    currentUsers,
                    headerAccessor.getMessageHeaders()
            );
        }
    }

    @EventListener
    public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        UserDTO user = sessionManager.getUserBySessionId(accessor.getSessionId()).orElseThrow(() -> {
            log.warn("User not found for sessionId {}", accessor.getSessionId());
            sessionManager.tryRemoveUser(sessionId);

            return new RuntimeException("Failed to send disconnect event");
        });
        UUID schemaId = sessionManager.getSchemaIdBySessionId(accessor.getSessionId()).orElseThrow(() -> {
            log.warn("Schema not found for sessionId {}", accessor.getSessionId());
            sessionManager.tryRemoveUser(sessionId);

            return new RuntimeException("Failed to send disconnect event");
        });

        if (sessionManager.tryRemoveUser(sessionId)) {
            messagingTemplate.convertAndSend(TOPIC_PREFIX + schemaId,
                new ServerEvent.ConnectionChangedEvent(
                    new ConnectionChangedPayload(schemaId, user, ConnectionChangedPayload.ConnectionChangeType.DISCONNECTED)
            ));
        }
    }
}
