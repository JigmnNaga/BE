package com.jigmnnaga.global.config;

import com.jigmnnaga.global.auth.StompAuthChannelInterceptor;
import com.jigmnnaga.global.exception.StompErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final long HEARTBEAT_INTERVAL_MILLIS = 10_000;

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final StompErrorHandler stompErrorHandler;

    private TaskScheduler messageBrokerTaskScheduler;

    @Autowired
    public void setMessageBrokerTaskScheduler(
            @Lazy @Qualifier("messageBrokerTaskScheduler") TaskScheduler messageBrokerTaskScheduler) {
        this.messageBrokerTaskScheduler = messageBrokerTaskScheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
        registry.setErrorHandler(stompErrorHandler);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 네트워크가 조용히 끊긴 연결을 빨리 감지하도록 서버·클라이언트가 10초마다 하트비트를 주고받는다.
        registry.enableSimpleBroker("/sub", "/queue")
                .setHeartbeatValue(new long[]{HEARTBEAT_INTERVAL_MILLIS, HEARTBEAT_INTERVAL_MILLIS})
                .setTaskScheduler(messageBrokerTaskScheduler);
        registry.setApplicationDestinationPrefixes("/pub");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
