package com.jigmnnaga.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.support.IntegrationTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class AppointmentStatsIntegrationTest extends IntegrationTestSupport {

    private static final String STATS_PATH = "/api/v1/members/me/stats";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private CreatedAppointment confirmedAppointment(Member host, String attendanceStatus) throws Exception {
        CreatedAppointment appointment = createAppointment(host);
        perform(put(appointmentPath(appointment) + "/confirmation/date-time"), host,
                Map.of("date", daysLater(10).toString(), "time", "15:00")).andExpect(status().isOk());
        perform(put(appointmentPath(appointment) + "/confirmation/location"), host, Map.of(
                "locationId", appointment.locationIds().getFirst(),
                "detailPlace", Map.of("placeName", "강남역 카페거리", "latitude", 37.4979, "longitude", 127.0276)))
                .andExpect(status().isOk());
        perform(put(appointmentPath(appointment) + "/attendance/me"), host, Map.of("attendanceStatus", attendanceStatus))
                .andExpect(status().isOk());
        return appointment;
    }

    private LocalDateTime moveToPast(CreatedAppointment appointment) {
        LocalDate pastDate = LocalDate.now().minusDays(3);
        jdbcTemplate.update("update appointment set confirmed_date = ? where id = ?", pastDate, appointment.id());
        return pastDate.atTime(15, 0);
    }

    private void arriveAt(CreatedAppointment appointment, Member member, LocalDateTime arrivedAt) {
        jdbcTemplate.update("update appointment_participant set departure_status = 'ARRIVED', arrived_at = ? "
                + "where appointment_id = ? and member_id = ?", arrivedAt, appointment.id(), member.getId());
    }

    @Test
    @DisplayName("지난 확정 약속 중 참석한 약속만 집계하고, 약속 시각(분 단위)까지 도착하면 제때 도착, 늦거나 도착 기록이 없으면 지각이다")
    void appointmentStats() throws Exception {
        Member member = createMember("김지금");

        CreatedAppointment early = confirmedAppointment(member, "ATTENDING");
        arriveAt(early, member, moveToPast(early).minusMinutes(5));

        CreatedAppointment sameMinute = confirmedAppointment(member, "ATTENDING");
        arriveAt(sameMinute, member, moveToPast(sameMinute).plusSeconds(40));

        CreatedAppointment late = confirmedAppointment(member, "ATTENDING");
        arriveAt(late, member, moveToPast(late).plusMinutes(12));

        moveToPast(confirmedAppointment(member, "ATTENDING"));

        moveToPast(confirmedAppointment(member, "NOT_ATTENDING"));
        confirmedAppointment(member, "ATTENDING");
        CreatedAppointment canceled = confirmedAppointment(member, "ATTENDING");
        perform(post(appointmentPath(canceled) + "/cancel"), member).andExpect(status().isOk());
        moveToPast(canceled);

        perform(get(STATS_PATH), member)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(4))
                .andExpect(jsonPath("$.data.onTimeCount").value(2))
                .andExpect(jsonPath("$.data.lateCount").value(2))
                .andExpect(jsonPath("$.data.onTimeRate").value(50.0));
    }

    @Test
    @DisplayName("제때 도착률은 소수 둘째 자리까지 반올림하고, 집계할 약속이 없으면 null이다")
    void onTimeRateRounding() throws Exception {
        Member member = createMember("김지금");
        perform(get(STATS_PATH), member)
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.onTimeRate").doesNotExist());

        for (int i = 0; i < 2; i++) {
            CreatedAppointment appointment = confirmedAppointment(member, "ATTENDING");
            arriveAt(appointment, member, moveToPast(appointment).minusMinutes(1));
        }
        moveToPast(confirmedAppointment(member, "ATTENDING"));

        perform(get(STATS_PATH), member)
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.onTimeCount").value(2))
                .andExpect(jsonPath("$.data.lateCount").value(1))
                .andExpect(jsonPath("$.data.onTimeRate").value(66.67));
    }
}
