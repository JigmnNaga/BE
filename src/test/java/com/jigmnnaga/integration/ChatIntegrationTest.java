package com.jigmnnaga.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.support.IntegrationTestSupport;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatIntegrationTest extends IntegrationTestSupport {

    @LocalServerPort
    private int port;

    private StompSession connect(Member member, BlockingQueue<String> errors) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        StompHeaders headers = new StompHeaders();
        if (member != null) {
            headers.add("Authorization", bearer(member));
        }
        return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), headers,
                new StompSessionHandlerAdapter() {
                    @Override
                    public Type getPayloadType(StompHeaders frameHeaders) {
                        return Map.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders frameHeaders, Object payload) {
                        errors.add(frameHeaders.getFirst("message"));
                    }
                }).get(5, TimeUnit.SECONDS);
    }

    private BlockingQueue<Map<?, ?>> subscribe(StompSession session, String destination) throws InterruptedException {
        BlockingQueue<Map<?, ?>> received = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((Map<?, ?>) payload);
            }
        });
        Thread.sleep(300);
        return received;
    }

    @Test
    @DisplayName("메시지를 보내면 같은 방 참여자에게 실시간으로 전달되고, 읽으면 안 읽은 사람 수가 줄어드는 이벤트가 전달된다")
    void sendAndReadMessage() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, member);
        StompSession hostSession = connect(host, new LinkedBlockingQueue<>());
        StompSession memberSession = connect(member, new LinkedBlockingQueue<>());
        BlockingQueue<Map<?, ?>> memberRoom = subscribe(memberSession, "/sub/appointments/" + appointment.id());
        BlockingQueue<Map<?, ?>> hostReadEvents = subscribe(hostSession, "/sub/appointments/" + appointment.id() + "/read");

        hostSession.send("/pub/appointments/" + appointment.id() + "/messages", Map.of("content", "다들 토요일 어때요?"));
        Map<?, ?> received = memberRoom.poll(3, TimeUnit.SECONDS);

        assertThat(received).isNotNull();
        assertThat(received.get("content")).isEqualTo("다들 토요일 어때요?");
        assertThat(received.get("senderNickname")).isEqualTo("태양");
        assertThat(received.get("unreadCount")).isEqualTo(1);

        memberSession.send("/pub/appointments/" + appointment.id() + "/read",
                Map.of("lastReadMessageId", received.get("messageId")));
        Map<?, ?> readEvent = hostReadEvents.poll(3, TimeUnit.SECONDS);

        assertThat(readEvent).isNotNull();
        assertThat(((Number) readEvent.get("memberId")).longValue()).isEqualTo(member.getId());
        assertThat(readEvent.get("lastReadMessageId")).isEqualTo(received.get("messageId"));
    }

    @Test
    @DisplayName("참여하지 않은 채팅방은 구독할 수 없고, 토큰 없이는 연결할 수 없다")
    void rejectUnauthorizedAccess() throws Exception {
        Member host = createMember("태양");
        Member outsider = createMember("외부인");
        CreatedAppointment appointment = createAppointment(host);
        BlockingQueue<String> errors = new LinkedBlockingQueue<>();

        StompSession outsiderSession = connect(outsider, errors);
        subscribe(outsiderSession, "/sub/appointments/" + appointment.id());

        assertThat(errors.poll(3, TimeUnit.SECONDS)).isEqualTo("약속에 참여한 멤버만 이용할 수 있습니다.");
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> connect(null, new LinkedBlockingQueue<>())))
                .isInstanceOf(ExecutionException.class);
    }
}
