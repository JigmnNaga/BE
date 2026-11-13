package com.jigmnnaga.global.auth;

import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.domain.service.AppointmentParticipantGetService;
import com.jigmnnaga.domain.service.MemberGetService;
import com.jigmnnaga.global.jwt.JwtProvider;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern APPOINTMENT_TOPIC = Pattern.compile("^/sub/appointments/(\\d+)(/read|/location|/eta|/arrival)?$");
    private static final String USER_DESTINATION_PREFIX = "/user/";

    private final JwtProvider jwtProvider;
    private final MemberGetService memberGetService;
    private final AppointmentParticipantGetService appointmentParticipantGetService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.CONNECT) {
            accessor.setUser(new StompPrincipal(authenticate(accessor)));
        }
        if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            validateSubscription(accessor);
        }
        return message;
    }

    private Long authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long memberId = jwtProvider.getMemberIdFromAccessToken(authorization.substring(BEARER_PREFIX.length()));
        boolean isActiveMember = memberGetService.findById(memberId)
                .filter(member -> !member.isWithdrawn())
                .isPresent();
        if (!isActiveMember) {
            throw new BusinessException(ErrorCode.WITHDRAWN_MEMBER);
        }
        return memberId;
    }

    private void validateSubscription(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();
        if (!(user instanceof StompPrincipal principal) || destination == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (destination.startsWith(USER_DESTINATION_PREFIX)) {
            return;
        }

        Matcher matcher = APPOINTMENT_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            throw new BusinessException(ErrorCode.INVALID_SUBSCRIPTION_DESTINATION);
        }

        Long appointmentId = Long.valueOf(matcher.group(1));
        appointmentParticipantGetService.findAcceptedByAppointmentIdAndMemberId(appointmentId, principal.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_APPOINTMENT_PARTICIPANT));
    }
}
