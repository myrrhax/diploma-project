package com.github.myrrhax.diploma_project.security;

import com.github.myrrhax.diploma_project.model.dto.UserDTO;
import com.github.myrrhax.diploma_project.service.AuthorityCheckService;
import com.github.myrrhax.diploma_project.service.SchemaSessionManagerService;
import com.github.myrrhax.diploma_project.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSecurityChannelInterceptor implements ChannelInterceptor {
    private static final String SCHEMA_SUBSCRIPTION_TOPIC_PATTERN = "/topic/schema/{id}";
    private static final String SCHEMA_CONNECTIONS_TOPIC_PATTERN = "/topic/schema-connections/{id}";
    private final TokenAuthenticationDetailsService tokenDetailsService;
    private final AuthorityCheckService authorityCheckService;
    private final SchemaSessionManagerService schemaSessionManagerService;
    private final UserService userService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    @Transactional(readOnly = true)
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && accessor.getCommand() != null) {
            if (accessor.getCommand().equals(StompCommand.CONNECT)) {
                parseUserConnection(accessor);
            } else if (accessor.getCommand().equals(StompCommand.SUBSCRIBE)) {
                parseUserSubscription(accessor);
            }
        }

        return message;
    }

    private void parseUserConnection(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");

        if (header != null) {
            if (header.startsWith("Bearer ")) {
                String stringifyToken = header.substring(7);

                var authToken = new PreAuthenticatedAuthenticationToken(stringifyToken, "bearer");
                UserDetails user = tokenDetailsService.loadUserDetails(authToken);
                accessor.setUser(new PreAuthenticatedAuthenticationToken(user, "bearer", user.getAuthorities()));
            }
        }
    }

    private void parseUserSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (Objects.isNull(destination) || destination.isBlank()) {
            return;
        }
        String matchedPattern = null;

        if (pathMatcher.match(SCHEMA_SUBSCRIPTION_TOPIC_PATTERN, destination)) {
            matchedPattern = SCHEMA_SUBSCRIPTION_TOPIC_PATTERN;
        } else if (pathMatcher.match(SCHEMA_CONNECTIONS_TOPIC_PATTERN, destination)) {
            matchedPattern = SCHEMA_CONNECTIONS_TOPIC_PATTERN;
        }

        if (matchedPattern != null) {
            Map<String, String> variables = pathMatcher.extractUriTemplateVariables(matchedPattern,
                    Objects.requireNonNull(accessor.getDestination()));
            String schemaId = variables.get("id");
            UUID parsedSchemaId;

            try {
                parsedSchemaId = UUID.fromString(schemaId);
            } catch (IllegalArgumentException e) {
                throw new MessageDeliveryException("Invalid schema id " + schemaId);
            }
            PreAuthenticatedAuthenticationToken token = (PreAuthenticatedAuthenticationToken) accessor.getUser();

            if (token == null) {
                log.error("Failed to extract user from accessor");
                throw new MessageDeliveryException("Failed to extract user from accessor");
            }

            TokenUser tokenUser = (TokenUser) token.getPrincipal();
            if (tokenUser == null) {
                throw new MessageDeliveryException("Invalid token");
            }

            if (matchedPattern.equals(SCHEMA_SUBSCRIPTION_TOPIC_PATTERN)) {
                handleSchemaEventsSubscription(parsedSchemaId, tokenUser.getToken().userId(), accessor.getSessionId());
            } else {
                handleSchemaConnectionsSubscription(parsedSchemaId, accessor.getSessionId());
            }
        }
    }

    private void handleSchemaConnectionsSubscription(UUID schemaId, String sessionId) {
        if (!schemaSessionManagerService.isConnected(sessionId, schemaId)) {
            throw new MessageDeliveryException("User is not connected to schema events topic");
        }
    }

    private void handleSchemaEventsSubscription(UUID schemaId, UUID userId, String sessionId) {
        if (!authorityCheckService.hasAccess(userId, schemaId)) {
            throw new MessageDeliveryException("Access denied");
        }

        if (!schemaSessionManagerService.tryAddUser(schemaId, sessionId, userId)) {
            throw new MessageDeliveryException("Too many online users in scheme");
        }
    }
}
