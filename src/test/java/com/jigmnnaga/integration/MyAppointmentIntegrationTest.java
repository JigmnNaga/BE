package com.jigmnnaga.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentCandidateDate;
import com.jigmnnaga.domain.entity.AppointmentCandidateLocation;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.DateSelectionType;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.ParticipantRole;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import com.jigmnnaga.domain.repository.AppointmentRepository;
import com.jigmnnaga.support.IntegrationTestSupport;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

class MyAppointmentIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AppointmentRepository appointmentRepository;

    private Long saveAppointment(Member member, String title, LocalDate candidateDate, LocalDate confirmedDate) {
        return transactionTemplate.execute(status -> {
            Appointment appointment = Appointment.builder()
                    .title(title)
                    .dateSelectionType(DateSelectionType.SINGLE)
                    .responseDeadline(candidateDate.minusDays(1).atStartOfDay())
                    .inviteCode(title + "-" + member.getId())
                    .build();
            appointment.addCandidateDate(AppointmentCandidateDate.builder().candidateDate(candidateDate).build());
            AppointmentCandidateLocation location = AppointmentCandidateLocation.builder()
                    .placeName("강남역").latitude(37.5).longitude(127.0).build();
            appointment.addCandidateLocation(location);
            appointment.addParticipant(AppointmentParticipant.builder()
                    .member(memberRepository.findById(member.getId()).orElseThrow())
                    .role(ParticipantRole.HOST)
                    .status(ParticipationStatus.ACCEPTED)
                    .build());
            appointmentRepository.save(appointment);
            if (confirmedDate != null) {
                appointment.confirmDateTime(confirmedDate, LocalTime.of(18, 0));
                appointment.confirmLocation(location, "스터디카페", null, null, null);
            }
            return appointment.getId();
        });
    }

    private List<String> titles(JsonNode items) {
        List<String> titles = new ArrayList<>();
        items.forEach(item -> titles.add(item.get("title").asString()));
        return titles;
    }

    @Test
    @DisplayName("다가오는 약속은 가까운 순, 지난 약속은 최근 순으로 조회되고 다음 약속은 가장 가까운 확정 약속이다")
    void myAppointments() throws Exception {
        Member member = createMember("나");
        saveAppointment(member, "3일 뒤 확정", daysLater(3), daysLater(3));
        saveAppointment(member, "1일 뒤 확정", daysLater(1), daysLater(1));
        saveAppointment(member, "5일 뒤 조율 중", daysLater(5), null);
        saveAppointment(member, "3일 전 확정", daysLater(-3), daysLater(-3));
        saveAppointment(member, "10일 전 조율 만료", daysLater(-10), null);

        JsonNode upcoming = data(perform(get("/api/v1/appointments/me").param("type", "UPCOMING"), member));
        JsonNode past = data(perform(get("/api/v1/appointments/me").param("type", "PAST"), member));
        JsonNode next = data(perform(get("/api/v1/appointments/me/next"), member));

        assertThat(titles(upcoming)).containsExactly("1일 뒤 확정", "3일 뒤 확정", "5일 뒤 조율 중");
        assertThat(titles(past)).containsExactly("3일 전 확정", "10일 전 조율 만료");
        assertThat(next.get("title").asString()).isEqualTo("1일 뒤 확정");
    }

    @Test
    @DisplayName("월별 캘린더는 해당 월에 날짜가 확정된 약속만 날짜별로 반환한다")
    void calendar() throws Exception {
        Member member = createMember("나");
        LocalDate confirmedDate = daysLater(2);
        saveAppointment(member, "확정 약속", confirmedDate, confirmedDate);
        saveAppointment(member, "조율 중 약속", confirmedDate, null);

        JsonNode calendar = data(perform(get("/api/v1/appointments/me/calendar")
                .param("month", YearMonth.from(confirmedDate).toString()), member));

        List<String> titles = new ArrayList<>();
        calendar.get("days").forEach(day -> titles.addAll(titles(day.get("appointments"))));
        assertThat(titles).containsExactly("확정 약속");
        perform(get("/api/v1/appointments/me").param("type", "UNKNOWN"), member).andExpect(status().isBadRequest());
    }
}
