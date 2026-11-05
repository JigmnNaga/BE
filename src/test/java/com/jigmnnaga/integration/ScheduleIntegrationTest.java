package com.jigmnnaga.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ScheduleIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AppointmentRepository appointmentRepository;

    private Map<String, Object> schedule(LocalDate date, String start, String end, long locationId) {
        return Map.of("availabilities", List.of(Map.of("date", date.toString(), "startTime", start, "endTime", end)),
                "preferredLocationId", locationId);
    }

    @Test
    @DisplayName("가능 시간을 다시 제출하면 기존 선택이 교체된다")
    void submitAndResubmitSchedule() throws Exception {
        Member host = createMember("태양");
        LocalDate date = daysLater(10);
        CreatedAppointment appointment = createAppointment(host,
                createRequest("RANGE", List.of(date, date.plusDays(1)), List.of("강남역", "잠실역"), date.minusDays(1)));
        String path = appointmentPath(appointment) + "/schedules/me";

        perform(put(path), host, schedule(date, "12:00", "18:00", appointment.locationIds().get(0)))
                .andExpect(status().isOk());
        perform(put(path), host, schedule(date.plusDays(1), "09:00", "12:00", appointment.locationIds().get(1)))
                .andExpect(status().isOk());

        perform(get(path), host)
                .andExpect(jsonPath("$.data.submitted").value(true))
                .andExpect(jsonPath("$.data.availabilities.length()").value(1))
                .andExpect(jsonPath("$.data.availabilities[0].startTime").value("09:00"))
                .andExpect(jsonPath("$.data.preferredLocationId").value(appointment.locationIds().get(1)));
    }

    @Test
    @DisplayName("후보 날짜 밖, 시작 시간이 종료 시간 이후, 다른 약속의 장소는 제출할 수 없다")
    void submitScheduleValidation() throws Exception {
        Member host = createMember("태양");
        LocalDate date = daysLater(10);
        CreatedAppointment appointment = createAppointment(host);
        CreatedAppointment other = createAppointment(host);
        String path = appointmentPath(appointment) + "/schedules/me";

        perform(put(path), host, schedule(date.plusDays(3), "12:00", "18:00", appointment.locationIds().getFirst()))
                .andExpect(status().isBadRequest());
        perform(put(path), host, schedule(date, "18:00", "12:00", appointment.locationIds().getFirst()))
                .andExpect(status().isBadRequest());
        perform(put(path), host, schedule(date, "12:00", "18:00", other.locationIds().getFirst()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("응답 현황은 선택 완료 인원과 장소별 득표 수·투표자·최다 득표 여부를 반환한다")
    void scheduleStatus() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        Member late = createMember("하은");
        LocalDate date = daysLater(10);
        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, member);
        accept(appointment, late);
        long gangnam = appointment.locationIds().get(0);

        perform(put(appointmentPath(appointment) + "/schedules/me"), host, schedule(date, "12:00", "18:00", gangnam));
        perform(put(appointmentPath(appointment) + "/schedules/me"), member, schedule(date, "13:00", "15:00", gangnam));

        perform(get(appointmentPath(appointment) + "/schedules"), host)
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.submittedCount").value(2))
                .andExpect(jsonPath("$.data.locationVotes[0].voteCount").value(2))
                .andExpect(jsonPath("$.data.locationVotes[0].topVoted").value(true))
                .andExpect(jsonPath("$.data.locationVotes[0].voters.length()").value(2))
                .andExpect(jsonPath("$.data.locationVotes[1].voteCount").value(0))
                .andExpect(jsonPath("$.data.locationVotes[1].topVoted").value(false));
    }

    @Test
    @DisplayName("히트맵은 슬롯 전체를 덮는 가능 시간만 집계하고 가능 인원이 많은 시간대를 상위 후보로 반환한다")
    void heatmap() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        LocalDate date = daysLater(10);
        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, member);
        long gangnam = appointment.locationIds().getFirst();
        perform(put(appointmentPath(appointment) + "/schedules/me"), host, schedule(date, "12:00", "17:30", gangnam));
        perform(put(appointmentPath(appointment) + "/schedules/me"), member, schedule(date, "14:00", "16:00", gangnam));

        perform(get(appointmentPath(appointment) + "/schedules/heatmap"), host)
                .andExpect(jsonPath("$.data.slotMinutes").value(60))
                .andExpect(jsonPath("$.data.dates[0].slots[0].startTime").value("12:00"))
                .andExpect(jsonPath("$.data.dates[0].slots[2].availableCount").value(2))
                .andExpect(jsonPath("$.data.dates[0].slots[5].startTime").value("17:00"))
                .andExpect(jsonPath("$.data.dates[0].slots[5].availableCount").value(0))
                .andExpect(jsonPath("$.data.topCandidates[0].startTime").value("14:00"))
                .andExpect(jsonPath("$.data.topCandidates[0].endTime").value("16:00"))
                .andExpect(jsonPath("$.data.topCandidates[0].availableCount").value(2));

        perform(get(appointmentPath(appointment) + "/schedules/heatmap").param("slotMinutes", "30"), host)
                .andExpect(jsonPath("$.data.dates[0].slots[10].startTime").value("17:00"))
                .andExpect(jsonPath("$.data.dates[0].slots[10].availableCount").value(1));
        perform(get(appointmentPath(appointment) + "/schedules/heatmap").param("slotMinutes", "45"), host)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("응답 기한이 지나면 가능 시간을 제출할 수 없다")
    void cannotSubmitAfterDeadline() throws Exception {
        Member host = createMember("태양");
        LocalDate date = daysLater(3);
        Long appointmentId = transactionTemplate.execute(status -> {
            Appointment appointment = Appointment.builder()
                    .title("기한 지난 약속")
                    .dateSelectionType(DateSelectionType.SINGLE)
                    .responseDeadline(LocalDateTime.now().minusHours(1))
                    .inviteCode("deadline-" + host.getId())
                    .build();
            appointment.addCandidateDate(AppointmentCandidateDate.builder().candidateDate(date).build());
            appointment.addCandidateLocation(AppointmentCandidateLocation.builder()
                    .placeName("강남역").latitude(37.5).longitude(127.0).build());
            appointment.addParticipant(AppointmentParticipant.builder()
                    .member(memberRepository.findById(host.getId()).orElseThrow())
                    .role(ParticipantRole.HOST)
                    .status(ParticipationStatus.ACCEPTED)
                    .build());
            return appointmentRepository.save(appointment).getId();
        });
        Long locationId = transactionTemplate.execute(status ->
                appointmentRepository.findById(appointmentId).orElseThrow().getCandidateLocations().getFirst().getId());

        perform(put("/api/v1/appointments/" + appointmentId + "/schedules/me"), host,
                schedule(date, "12:00", "18:00", locationId))
                .andExpect(status().isBadRequest());
    }
}
