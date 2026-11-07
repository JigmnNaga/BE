package com.jigmnnaga.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.jigmnnaga.application.usecase.ScheduledNotificationUseCase;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentCandidateDate;
import com.jigmnnaga.domain.entity.AppointmentCandidateLocation;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.AttendanceStatus;
import com.jigmnnaga.domain.entity.DateSelectionType;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.NotificationType;
import com.jigmnnaga.domain.entity.ParticipantRole;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import com.jigmnnaga.domain.repository.AppointmentRepository;
import com.jigmnnaga.domain.repository.NotificationRepository;
import com.jigmnnaga.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ScheduledNotificationUseCaseTest extends IntegrationTestSupport {

    @Autowired
    private ScheduledNotificationUseCase scheduledNotificationUseCase;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private final LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0);

    private Long saveAppointment(Member host, Member member, LocalDateTime deadline, LocalDateTime confirmedAt,
                                 AttendanceStatus memberAttendance) {
        return transactionTemplate.execute(status -> {
            Appointment appointment = Appointment.builder()
                    .title("예약 알림 테스트")
                    .dateSelectionType(DateSelectionType.SINGLE)
                    .responseDeadline(deadline)
                    .inviteCode("scheduled-" + host.getId() + "-" + System.nanoTime())
                    .build();
            appointment.addCandidateDate(AppointmentCandidateDate.builder()
                    .candidateDate(confirmedAt != null ? confirmedAt.toLocalDate() : deadline.toLocalDate().plusDays(1))
                    .build());
            AppointmentCandidateLocation location = AppointmentCandidateLocation.builder()
                    .placeName("강남역").latitude(37.5).longitude(127.0).build();
            appointment.addCandidateLocation(location);
            appointment.addParticipant(AppointmentParticipant.builder()
                    .member(memberRepository.findById(host.getId()).orElseThrow())
                    .role(ParticipantRole.HOST).status(ParticipationStatus.ACCEPTED).build());
            appointment.addParticipant(AppointmentParticipant.builder()
                    .member(memberRepository.findById(member.getId()).orElseThrow())
                    .role(ParticipantRole.MEMBER).status(ParticipationStatus.ACCEPTED).build());
            appointmentRepository.save(appointment);
            if (confirmedAt != null) {
                appointment.confirmDateTime(confirmedAt.toLocalDate(), confirmedAt.toLocalTime());
                appointment.confirmLocation(location, "스터디카페", null, null, null);
                if (memberAttendance != null) {
                    appointment.findAcceptedParticipant(member.getId()).orElseThrow().respondAttendance(memberAttendance);
                }
            }
            return appointment.getId();
        });
    }

    private List<String> received(Long appointmentId, NotificationType type) {
        return transactionTemplate.execute(status -> notificationRepository.findAll().stream()
                .filter(notification -> appointmentId.equals(notification.getAppointmentId()))
                .filter(notification -> notification.getType() == type)
                .map(notification -> notification.getMember().getNickname())
                .sorted()
                .toList());
    }

    @Test
    @DisplayName("응답 기한 24시간 이내인 약속의 미제출자에게 마감 임박 알림을 한 번만 보낸다")
    void deadlineReminder() {
        Member host = createMember("태양");
        Member member = createMember("태희");
        Long soon = saveAppointment(host, member, now.plusHours(10), null, null);
        Long later = saveAppointment(host, member, now.plusDays(3), null, null);

        scheduledNotificationUseCase.sendDeadlineReminders(now);
        scheduledNotificationUseCase.sendDeadlineReminders(now);

        assertThat(received(soon, NotificationType.VOTE_DEADLINE_SOON)).containsExactly("태양", "태희");
        assertThat(received(later, NotificationType.VOTE_DEADLINE_SOON)).isEmpty();
    }

    @Test
    @DisplayName("응답 기한이 지난 미확정 약속은 방장에게만 확정 요청 알림을 보내고, 확정된 약속은 보내지 않는다")
    void deadlineClosed() {
        Member host = createMember("태양");
        Member member = createMember("태희");
        Long unconfirmed = saveAppointment(host, member, now.minusHours(1), null, null);
        Long confirmed = saveAppointment(host, member, now.minusHours(1), now.plusDays(2), null);

        scheduledNotificationUseCase.sendDeadlineClosedNotifications(now);

        assertThat(received(unconfirmed, NotificationType.RESPONSE_DEADLINE_CLOSED)).containsExactly("태양");
        assertThat(received(confirmed, NotificationType.RESPONSE_DEADLINE_CLOSED)).isEmpty();
    }

    @Test
    @DisplayName("약속 5시간 이내에는 불참 응답자를 제외한 참여자에게 알림을 보내고, 시간이 바뀌면 다시 보낸다")
    void preAppointment() {
        Member host = createMember("태양");
        Member notAttending = createMember("태희");
        Long appointmentId = saveAppointment(host, notAttending, now.minusDays(1), now.plusHours(3),
                AttendanceStatus.NOT_ATTENDING);

        scheduledNotificationUseCase.sendPreAppointmentNotifications(now);
        scheduledNotificationUseCase.sendPreAppointmentNotifications(now);
        assertThat(received(appointmentId, NotificationType.APPOINTMENT_SOON)).containsExactly("태양");

        transactionTemplate.executeWithoutResult(status -> {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
            appointment.confirmDateTime(appointment.getConfirmedDate(), appointment.getConfirmedTime().plusHours(1));
        });
        scheduledNotificationUseCase.sendPreAppointmentNotifications(now);

        assertThat(received(appointmentId, NotificationType.APPOINTMENT_SOON)).hasSize(3);
    }
}
