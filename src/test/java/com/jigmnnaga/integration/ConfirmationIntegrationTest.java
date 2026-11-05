package com.jigmnnaga.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.NotificationType;
import com.jigmnnaga.domain.repository.NotificationRepository;
import com.jigmnnaga.support.IntegrationTestSupport;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

class ConfirmationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private NotificationRepository notificationRepository;

    private List<NotificationType> notificationTypes(Member member, long appointmentId) {
        return transactionTemplate.execute(status -> notificationRepository.findAll().stream()
                .filter(notification -> notification.getMember().getId().equals(member.getId()))
                .filter(notification -> Long.valueOf(appointmentId).equals(notification.getAppointmentId()))
                .map(notification -> notification.getType())
                .toList());
    }

    private Map<String, Object> locationWithDetail(long locationId, String detail) {
        Map<String, Object> request = new HashMap<>();
        request.put("locationId", locationId);
        if (detail != null) {
            request.put("detailPlace", Map.of("placeName", detail, "latitude", 37.5, "longitude", 127.0));
        }
        return request;
    }

    @Test
    @DisplayName("시간과 세부 장소까지 정해져야 확정되고, 확정 시 참여자에게 확정·참석 요청 알림이 간다")
    void confirmRequiresDetailPlace() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        LocalDate date = daysLater(10);
        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, member);
        String path = appointmentPath(appointment);

        perform(put(path + "/confirmation/date-time"), host, Map.of("date", date.toString(), "time", "17:00"));
        perform(put(path + "/confirmation/location"), host, locationWithDetail(appointment.locationIds().getFirst(), null));
        perform(get(path), member).andExpect(jsonPath("$.data.confirmed").value(false));
        assertThat(notificationTypes(member, appointment.id())).isEmpty();
        perform(put(path + "/attendance/me"), member, Map.of("attendanceStatus", "ATTENDING"))
                .andExpect(status().isBadRequest());

        perform(put(path + "/confirmation/location"), host, locationWithDetail(appointment.locationIds().getFirst(), "스터디카페"));

        perform(get(path), member)
                .andExpect(jsonPath("$.data.confirmed").value(true))
                .andExpect(jsonPath("$.data.detailPlace.placeName").value("스터디카페"));
        assertThat(notificationTypes(member, appointment.id()))
                .containsExactlyInAnyOrder(NotificationType.APPOINTMENT_CONFIRMED, NotificationType.ATTENDANCE_REQUESTED);
        assertThat(notificationTypes(host, appointment.id())).isEmpty();
        JsonNode messages = data(perform(get(path + "/messages"), member)).get("messages");
        assertThat(messages.get(messages.size() - 1).get("content").asString()).startsWith("약속이 확정됐어요");
    }

    @Test
    @DisplayName("확정 후 장소를 바꾸면 참석 응답이 초기화되고 변경 알림이 간다")
    void changeConfirmedLocationResetsAttendance() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, member);
        String path = appointmentPath(appointment);
        perform(put(path + "/confirmation/date-time"), host, Map.of("date", daysLater(10).toString(), "time", "17:00"));
        perform(put(path + "/confirmation/location"), host, locationWithDetail(appointment.locationIds().get(0), "스터디카페"));
        perform(put(path + "/attendance/me"), member, Map.of("attendanceStatus", "ATTENDING")).andExpect(status().isOk());

        perform(put(path + "/confirmation/location"), host, locationWithDetail(appointment.locationIds().get(1), "카페"));

        perform(get(path), member).andExpect(jsonPath("$.data.myAttendanceStatus").value("PENDING"));
        assertThat(notificationTypes(member, appointment.id())).contains(NotificationType.APPOINTMENT_CHANGED);
    }

    @Test
    @DisplayName("일정 조율 요청 시 방장에게 알림이 가고, 같은 요청을 반복하면 다시 보내지 않는다")
    void rescheduleRequestNotifiesHost() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, member);
        String path = appointmentPath(appointment);
        perform(put(path + "/confirmation/date-time"), host, Map.of("date", daysLater(10).toString(), "time", "17:00"));
        perform(put(path + "/confirmation/location"), host, locationWithDetail(appointment.locationIds().getFirst(), "스터디카페"));

        perform(put(path + "/attendance/me"), member, Map.of("attendanceStatus", "RESCHEDULE_REQUESTED"))
                .andExpect(status().isOk());
        perform(put(path + "/attendance/me"), member, Map.of("attendanceStatus", "RESCHEDULE_REQUESTED"))
                .andExpect(status().isOk());

        assertThat(notificationTypes(host, appointment.id())).containsExactly(NotificationType.RESCHEDULE_REQUESTED);
        perform(put(path + "/attendance/me"), member, Map.of("attendanceStatus", "PENDING"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("방장이 아니면 일정을 확정할 수 없고, 후보 날짜가 아닌 날짜는 확정할 수 없다")
    void confirmPermissionAndValidation() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, member);
        String path = appointmentPath(appointment) + "/confirmation/date-time";

        perform(put(path), member, Map.of("date", daysLater(10).toString(), "time", "17:00"))
                .andExpect(status().isForbidden());
        perform(put(path), host, Map.of("date", daysLater(20).toString(), "time", "17:00"))
                .andExpect(status().isBadRequest());
    }
}
