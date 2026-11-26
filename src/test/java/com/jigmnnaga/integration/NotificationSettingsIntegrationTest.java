package com.jigmnnaga.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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

@SuppressWarnings("unchecked")
class NotificationSettingsIntegrationTest extends IntegrationTestSupport {

    private static final String SETTINGS_URL = "/api/v1/members/me/notification-settings";

    // ───────────────────────────── 조회 ─────────────────────────────

    @Test
    @DisplayName("신규 회원의 기본 알림 설정은 전체·약속·출발·도착 ON, 마케팅 OFF이다")
    void defaultSettings() throws Exception {
        Member member = createMember("태양");

        perform(get(SETTINGS_URL), member)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allNotificationsEnabled").value(true))
                .andExpect(jsonPath("$.data.appointmentReminderEnabled").value(true))
                .andExpect(jsonPath("$.data.departureTimeEnabled").value(true))
                .andExpect(jsonPath("$.data.friendArrivalEnabled").value(true))
                .andExpect(jsonPath("$.data.marketingEnabled").value(false));
    }

    @Test
    @DisplayName("인증 없이 조회하면 401을 반환한다")
    void getSettingsUnauthorized() throws Exception {
        perform(get(SETTINGS_URL), null).andExpect(status().isUnauthorized());
    }

    // ───────────────────────────── 수정 ─────────────────────────────

    @Test
    @DisplayName("알림 설정을 저장하면 변경된 설정이 반환되고 재조회 시에도 유지된다")
    void updateAndRetrieve() throws Exception {
        Member member = createMember("태양");

        perform(put(SETTINGS_URL), member, Map.of(
                "allNotificationsEnabled", true,
                "appointmentReminderEnabled", false,
                "departureTimeEnabled", true,
                "friendArrivalEnabled", false,
                "marketingEnabled", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointmentReminderEnabled").value(false))
                .andExpect(jsonPath("$.data.friendArrivalEnabled").value(false))
                .andExpect(jsonPath("$.data.marketingEnabled").value(true));

        perform(get(SETTINGS_URL), member)
                .andExpect(jsonPath("$.data.appointmentReminderEnabled").value(false))
                .andExpect(jsonPath("$.data.friendArrivalEnabled").value(false))
                .andExpect(jsonPath("$.data.marketingEnabled").value(true));
    }

    @Test
    @DisplayName("알림 설정은 회원별로 독립적으로 관리된다")
    void settingsArePerMember() throws Exception {
        Member a = createMember("태양");
        Member b = createMember("하은");

        perform(put(SETTINGS_URL), a, Map.of(
                "allNotificationsEnabled", false,
                "appointmentReminderEnabled", false,
                "departureTimeEnabled", false,
                "friendArrivalEnabled", false,
                "marketingEnabled", false));

        perform(get(SETTINGS_URL), b)
                .andExpect(jsonPath("$.data.allNotificationsEnabled").value(true))
                .andExpect(jsonPath("$.data.appointmentReminderEnabled").value(true));
    }

    @Test
    @DisplayName("필드가 누락되면 400을 반환한다")
    void missingField() throws Exception {
        Member member = createMember("태양");

        perform(put(SETTINGS_URL), member, Map.of(
                "allNotificationsEnabled", true,
                "appointmentReminderEnabled", true))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인증 없이 수정하면 401을 반환한다")
    void updateSettingsUnauthorized() throws Exception {
        perform(put(SETTINGS_URL), null, Map.of(
                "allNotificationsEnabled", true,
                "appointmentReminderEnabled", true,
                "departureTimeEnabled", true,
                "friendArrivalEnabled", true,
                "marketingEnabled", false))
                .andExpect(status().isUnauthorized());
    }

    // ───────────────────────── FCM 발송 필터링 ─────────────────────────

    @Test
    @DisplayName("전체 알림 OFF 회원은 모든 FCM 푸시에서 제외된다")
    void allNotificationsOff_blocksPush() throws Exception {
        when(fcmClient.isEnabled()).thenReturn(true);
        when(fcmClient.send(anyList(), anyString(), anyString(), anyMap(), anyBoolean())).thenReturn(List.of());

        Member host = createMember("방장");
        Member allOff = createMember("전체OFF");
        Member active = createMember("활성");
        String allOffToken = "token-alloff-" + allOff.getId();
        String activeToken = "token-active-" + active.getId();

        perform(put("/api/v1/devices/token"), allOff, Map.of("token", allOffToken, "platform", "ANDROID"));
        perform(put("/api/v1/devices/token"), active, Map.of("token", activeToken, "platform", "IOS"));

        perform(put(SETTINGS_URL), allOff, allOffSettings());

        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, allOff);
        accept(appointment, active);

        perform(post(appointmentPath(appointment) + "/reminders"), host).andExpect(status().isOk());

        ArgumentCaptor<List<String>> tokens = ArgumentCaptor.forClass(List.class);
        verify(fcmClient, timeout(3000)).send(tokens.capture(), anyString(), anyString(), anyMap(), anyBoolean());
        assertThat(tokens.getValue()).containsExactly(activeToken);

        // DB 알림은 전체 알림 OFF 여부와 무관하게 저장된다
        perform(get("/api/v1/notifications/unread-count"), allOff)
                .andExpect(jsonPath("$.data.count").value(1));
    }

    @Test
    @DisplayName("약속 알림 OFF 회원은 약속 관련 FCM에서 제외되지만 알림함에는 저장된다")
    void appointmentReminderOff_blocksPush() throws Exception {
        when(fcmClient.isEnabled()).thenReturn(true);
        when(fcmClient.send(anyList(), anyString(), anyString(), anyMap(), anyBoolean())).thenReturn(List.of());

        Member host = createMember("방장");
        Member reminderOff = createMember("약속알림OFF");
        Member active = createMember("활성");
        String reminderOffToken = "token-remoff-" + reminderOff.getId();
        String activeToken = "token-act2-" + active.getId();

        perform(put("/api/v1/devices/token"), reminderOff, Map.of("token", reminderOffToken, "platform", "ANDROID"));
        perform(put("/api/v1/devices/token"), active, Map.of("token", activeToken, "platform", "IOS"));

        perform(put(SETTINGS_URL), reminderOff, Map.of(
                "allNotificationsEnabled", true,
                "appointmentReminderEnabled", false,
                "departureTimeEnabled", true,
                "friendArrivalEnabled", true,
                "marketingEnabled", false));

        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, reminderOff);
        accept(appointment, active);

        perform(post(appointmentPath(appointment) + "/reminders"), host).andExpect(status().isOk());

        ArgumentCaptor<List<String>> tokens = ArgumentCaptor.forClass(List.class);
        verify(fcmClient, timeout(3000)).send(tokens.capture(), anyString(), anyString(), anyMap(), anyBoolean());
        assertThat(tokens.getValue()).doesNotContain(reminderOffToken);
        assertThat(tokens.getValue()).contains(activeToken);

        // 알림함에는 저장됨
        perform(get("/api/v1/notifications/unread-count"), reminderOff)
                .andExpect(jsonPath("$.data.count").value(1));
    }

    @Test
    @DisplayName("약속방 알림 OFF와 전역 알림 설정이 함께 적용된다")
    void appointmentMuteAndGlobalSettingsBothApply() throws Exception {
        when(fcmClient.isEnabled()).thenReturn(true);
        when(fcmClient.send(anyList(), anyString(), anyString(), anyMap(), anyBoolean())).thenReturn(List.of());

        Member host = createMember("방장");
        Member mutedGlobally = createMember("전역꺼짐");
        Member mutedLocally = createMember("약속방꺼짐");
        Member active = createMember("활성");

        String mutedGloballyToken = "token-glob-" + mutedGlobally.getId();
        String mutedLocallyToken = "token-local-" + mutedLocally.getId();
        String activeToken = "token-act3-" + active.getId();

        perform(put("/api/v1/devices/token"), mutedGlobally, Map.of("token", mutedGloballyToken, "platform", "ANDROID"));
        perform(put("/api/v1/devices/token"), mutedLocally, Map.of("token", mutedLocallyToken, "platform", "ANDROID"));
        perform(put("/api/v1/devices/token"), active, Map.of("token", activeToken, "platform", "IOS"));

        perform(put(SETTINGS_URL), mutedGlobally, allOffSettings());

        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, mutedGlobally);
        accept(appointment, mutedLocally);
        accept(appointment, active);

        perform(patch(appointmentPath(appointment) + "/notification-setting"), mutedLocally, Map.of("enabled", false));

        perform(post(appointmentPath(appointment) + "/reminders"), host).andExpect(status().isOk());

        ArgumentCaptor<List<String>> tokens = ArgumentCaptor.forClass(List.class);
        verify(fcmClient, timeout(3000)).send(tokens.capture(), anyString(), anyString(), anyMap(), anyBoolean());
        assertThat(tokens.getValue()).containsExactly(activeToken);
    }

    // ─────────────────────── 헬퍼 ───────────────────────

    private Map<String, Object> allOffSettings() {
        return Map.of(
                "allNotificationsEnabled", false,
                "appointmentReminderEnabled", false,
                "departureTimeEnabled", false,
                "friendArrivalEnabled", false,
                "marketingEnabled", false);
    }
}
