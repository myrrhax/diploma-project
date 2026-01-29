package com.github.myrrhax.diploma_project.security;

import com.github.myrrhax.diploma_project.service.AuthorityService;
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
import org.springframework.util.AntPathMatcher;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSecurityChannelInterceptor implements ChannelInterceptor {
    private static final String SCHEMA_SUBSCRIPTION_TOPIC_PATTERN = "/topic/schema/{id}";
    private final JwsTokenProvider tokenProvider;
    private final TokenAuthenticationDetailsService tokenDetailsService;
    private final AuthorityService authorityService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
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
                Token token = tokenProvider.decodeToken(stringifyToken);

                var authToken = new PreAuthenticatedAuthenticationToken(token, "bearer");
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

        if (pathMatcher.match(SCHEMA_SUBSCRIPTION_TOPIC_PATTERN, destination)) {
            Map<String, String> variables = pathMatcher.extractUriTemplateVariables(SCHEMA_SUBSCRIPTION_TOPIC_PATTERN, destination);
            String schemaId = variables.get("id");
            try {
                UUID parsedId = UUID.fromString(schemaId);
                TokenUser tokenUser = (TokenUser) accessor.getUser();

                if (Objects.isNull(tokenUser)) {
                    throw new MessageDeliveryException("Invalid token");
                }

                if (!authorityService.hasAccess(tokenUser.getToken().userId(), parsedId)) {
                    throw new MessageDeliveryException("Access denied");
                }
            } catch (IllegalArgumentException e) {
                log.error("Failed to parse scheme id");
                throw new MessageDeliveryException("Failed to parse scheme id");
            }
        }
    }
}
