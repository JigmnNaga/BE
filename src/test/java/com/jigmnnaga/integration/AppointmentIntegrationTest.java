package com.jigmnnaga.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.repository.AppointmentRepository;
import com.jigmnnaga.domain.repository.ChatMessageRepository;
import com.jigmnnaga.support.IntegrationTestSupport;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

class AppointmentIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Test
    @DisplayName("약속을 생성하면 생성자가 방장이 되고 초대 링크와 채팅방 시스템 메시지가 만들어진다")
    void createAppointment() throws Exception {
        Member host = createMember("태양");
        LocalDate date = daysLater(10);

        perform(post("/api/v1/appointments"), host,
                createRequest("RANGE", List.of(date, date.plusDays(1)), List.of("강남역"), date.minusDays(1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.inviteCode").isNotEmpty())
                .andExpect(jsonPath("$.data.inviteUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.candidateDates.length()").value(2));

        CreatedAppointment appointment = createAppointment(host);
        perform(get(appointmentPath(appointment)), host)
                .andExpect(jsonPath("$.data.myRole").value("HOST"))
                .andExpect(jsonPath("$.data.joinedCount").value(1))
                .andExpect(jsonPath("$.data.confirmed").value(false));
        perform(get(appointmentPath(appointment) + "/messages"), host)
                .andExpect(jsonPath("$.data.messages[0].type").value("SYSTEM"))
                .andExpect(jsonPath("$.data.messages[0].content").value("채팅방이 열렸어요"));
    }

    @Test
    @DisplayName("약속 생성 시 후보 장소 6개, 연속되지 않은 기간, 첫 후보일 이후 응답 기한은 거절된다")
    void createAppointmentValidation() throws Exception {
        Member host = createMember("태양");
        LocalDate date = daysLater(10);

        perform(post("/api/v1/appointments"), host, createRequest("SINGLE", List.of(date),
                List.of("1역", "2역", "3역", "4역", "5역", "6역"), date.minusDays(1)))
                .andExpect(status().isBadRequest());
        perform(post("/api/v1/appointments"), host, createRequest("RANGE", List.of(date, date.plusDays(2)),
                List.of("강남역"), date.minusDays(1)))
                .andExpect(status().isBadRequest());
        perform(post("/api/v1/appointments"), host, createRequest("SINGLE", List.of(date),
                List.of("강남역"), date))
                .andExpect(status().isBadRequest());
        perform(post("/api/v1/appointments"), null, createRequest("SINGLE", List.of(date),
                List.of("강남역"), date.minusDays(1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("이전 약속 멤버를 불러오면 바로 초대되어 받은 초대 목록에 나타난다")
    void inviteFromPreviousAppointment() throws Exception {
        Member host = createMember("태양");
        Member friend = createMember("태희");
        Member declined = createMember("채원");
        CreatedAppointment previous = createAppointment(host);
        accept(previous, friend);
        perform(post("/api/v1/invites/" + previous.inviteCode() + "/decline"), declined);

        LocalDate date = daysLater(12);
        Map<String, Object> request = createRequest("SINGLE", List.of(date), List.of("강남역"), date.minusDays(1));
        request.put("previousMembers", Map.of("appointmentId", previous.id(), "memberIds", List.of(friend.getId())));
        CreatedAppointment rebooked = createAppointment(host, request);

        perform(get("/api/v1/invites/me"), friend)
                .andExpect(jsonPath("$.data[0].appointmentId").value(rebooked.id()))
                .andExpect(jsonPath("$.data[0].hostNickname").value("태양"));

        request.put("previousMembers", Map.of("appointmentId", previous.id(), "memberIds", List.of(declined.getId())));
        perform(post("/api/v1/appointments"), host, request).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("후보 날짜를 바꾸면 제외된 날짜의 가능 시간이 삭제되고 응답 기한 이전 날짜로는 바꿀 수 없다")
    void updateCandidateDates() throws Exception {
        Member host = createMember("태양");
        LocalDate date = daysLater(10);
        CreatedAppointment appointment = createAppointment(host,
                createRequest("RANGE", List.of(date, date.plusDays(1)), List.of("강남역"), date.minusDays(1)));
        perform(put(appointmentPath(appointment) + "/schedules/me"), host, Map.of(
                "availabilities", List.of(
                        Map.of("date", date.toString(), "startTime", "12:00", "endTime", "18:00"),
                        Map.of("date", date.plusDays(1).toString(), "startTime", "12:00", "endTime", "18:00")),
                "preferredLocationId", appointment.locationIds().getFirst()));

        perform(put(appointmentPath(appointment) + "/candidate-dates"), host,
                Map.of("dateSelectionType", "SINGLE", "candidateDates", List.of(date.plusDays(1).toString())))
                .andExpect(status().isOk());
        perform(get(appointmentPath(appointment) + "/schedules/me"), host)
                .andExpect(jsonPath("$.data.availabilities.length()").value(1))
                .andExpect(jsonPath("$.data.availabilities[0].date").value(date.plusDays(1).toString()));

        perform(put(appointmentPath(appointment) + "/candidate-dates"), host,
                Map.of("dateSelectionType", "SINGLE", "candidateDates", List.of(date.minusDays(1).toString())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("방장이 아니면 약속을 수정할 수 없고, 취소된 약속은 더 이상 수정할 수 없다")
    void updateAndCancelPermission() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, member);

        perform(patch(appointmentPath(appointment) + "/title"), member, Map.of("title", "바뀐 이름"))
                .andExpect(status().isForbidden());
        perform(patch(appointmentPath(appointment) + "/title"), host, Map.of("title", "바뀐 이름"))
                .andExpect(status().isOk());

        perform(post(appointmentPath(appointment) + "/cancel"), host).andExpect(status().isOk());
        perform(get(appointmentPath(appointment)), member).andExpect(jsonPath("$.data.canceled").value(true));
        perform(patch(appointmentPath(appointment) + "/title"), host, Map.of("title", "또 바뀐 이름"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("약속방을 삭제하면 약속과 채팅 메시지가 함께 삭제된다")
    void deleteAppointment() throws Exception {
        Member host = createMember("태양");
        CreatedAppointment appointment = createAppointment(host);

        perform(delete(appointmentPath(appointment)), host).andExpect(status().isOk());

        assertThat(appointmentRepository.findById(appointment.id())).isEmpty();
        long remainingMessages = transactionTemplate.execute(status -> chatMessageRepository.findAll().stream()
                .filter(message -> message.getAppointment().getId().equals(appointment.id()))
                .count());
        assertThat(remainingMessages).isZero();
        perform(get(appointmentPath(appointment)), host).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("약속 상세 조회는 수락한 참여자만 가능하다")
    void detailPermission() throws Exception {
        Member host = createMember("태양");
        Member outsider = createMember("외부인");
        CreatedAppointment appointment = createAppointment(host);

        JsonNode detail = data(perform(get(appointmentPath(appointment)), host));
        assertThat(detail.get("candidateLocations")).hasSize(2);
        perform(get(appointmentPath(appointment)), outsider).andExpect(status().isForbidden());
    }
}
