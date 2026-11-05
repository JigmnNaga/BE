package com.jigmnnaga.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.support.IntegrationTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;

class NotificationIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("미응답자 알림은 가능 시간을 제출하지 않은 멤버에게만 가고 알림함에서 읽음 처리할 수 있다")
    void reminderAndInbox() throws Exception {
        Member host = createMember("태양");
        Member submitted = createMember("태희");
        Member pending = createMember("하은");
        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, submitted);
        accept(appointment, pending);
        perform(put(appointmentPath(appointment) + "/schedules/me"), submitted, Map.of(
                "availabilities", List.of(Map.of("date", daysLater(10).toString(), "startTime", "12:00", "endTime", "18:00")),
                "preferredLocationId", appointment.locationIds().getFirst()));

        perform(post(appointmentPath(appointment) + "/reminders"), host)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("SCHEDULE_INPUT_REQUESTED"))
                .andExpect(jsonPath("$.data.sentCount").value(1));

        perform(get("/api/v1/notifications/unread-count"), submitted).andExpect(jsonPath("$.data.count").value(0));
        JsonNode inbox = data(perform(get("/api/v1/notifications"), pending));
        assertThat(inbox.get("notifications")).hasSize(1);
        long notificationId = inbox.get("notifications").get(0).get("notificationId").asLong();

        perform(patch("/api/v1/notifications/" + notificationId + "/read"), submitted).andExpect(status().isNotFound());
        perform(patch("/api/v1/notifications/" + notificationId + "/read"), pending).andExpect(status().isOk());
        perform(get("/api/v1/notifications/unread-count"), pending).andExpect(jsonPath("$.data.count").value(0));
    }

    @Test
    @DisplayName("방장이 아니면 미응답자 알림을 보낼 수 없다")
    void reminderPermission() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, member);

        perform(post(appointmentPath(appointment) + "/reminders"), member).andExpect(status().isForbidden());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("알림을 보내면 등록된 기기로 푸시가 가고, 약속방 알림을 끈 멤버는 푸시에서 제외된다")
    void pushExcludesMutedMembers() throws Exception {
        when(fcmClient.isEnabled()).thenReturn(true);
        when(fcmClient.send(anyList(), anyString(), anyString(), anyMap(), anyBoolean())).thenReturn(List.of());
        Member host = createMember("태양");
        Member muted = createMember("태희");
        Member active = createMember("하은");
        String mutedToken = "muted-token-" + muted.getId();
        String activeToken = "active-token-" + active.getId();
        perform(put("/api/v1/devices/token"), muted, Map.of("token", mutedToken, "platform", "ANDROID"));
        perform(put("/api/v1/devices/token"), active, Map.of("token", activeToken, "platform", "IOS"));
        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, muted);
        accept(appointment, active);
        perform(patch(appointmentPath(appointment) + "/notification-setting"), muted, Map.of("enabled", false));

        perform(post(appointmentPath(appointment) + "/reminders"), host).andExpect(status().isOk());

        ArgumentCaptor<List<String>> tokens = ArgumentCaptor.forClass(List.class);
        verify(fcmClient, timeout(3000)).send(tokens.capture(), eq("가능한 일정을 알려주세요"), anyString(), anyMap(), eq(false));
        assertThat(tokens.getValue()).containsExactly(activeToken);
        perform(get("/api/v1/notifications/unread-count"), muted).andExpect(jsonPath("$.data.count").value(1));
    }

    @Test
    @DisplayName("기기 토큰은 본인 것만 삭제할 수 있다")
    void deleteDeviceToken() throws Exception {
        Member owner = createMember("나");
        Member other = createMember("남");
        String token = "device-token-" + owner.getId();
        perform(put("/api/v1/devices/token"), owner, Map.of("token", token, "platform", "ANDROID"))
                .andExpect(status().isOk());

        perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/devices/token")
                .param("token", token), other).andExpect(status().isNotFound());
        perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/devices/token")
                .param("token", token), owner).andExpect(status().isOk());
    }
}
