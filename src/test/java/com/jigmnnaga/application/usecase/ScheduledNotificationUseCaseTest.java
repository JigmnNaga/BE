package com.jigmnnaga.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.jigmnnaga.application.dto.request.AttendanceRespondRequest;
import com.jigmnnaga.application.dto.request.ConfirmLocationRequest;
import com.jigmnnaga.application.dto.request.ConfirmLocationRequest.DetailPlaceRequest;
import com.jigmnnaga.application.usecase.AppointmentConfirmUseCase;
import com.jigmnnaga.application.usecase.AttendanceRespondUseCase;
import com.jigmnnaga.application.usecase.ReminderSendUseCase;
import com.jigmnnaga.application.usecase.ScheduledNotificationUseCase;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentCandidateDate;
import com.jigmnnaga.domain.entity.AppointmentCandidateLocation;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.AttendanceStatus;
import com.jigmnnaga.domain.entity.DateSelectionType;
import com.jigmnnaga.domain.entity.DepartureUpcomingStage;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.NotificationType;
import com.jigmnnaga.domain.entity.ParticipantRole;
import com.jigmnnaga.domain.entity.ParticipationStatus;
import com.jigmnnaga.domain.entity.TravelMode;
import com.jigmnnaga.domain.repository.AppointmentRepository;
import com.jigmnnaga.domain.repository.NotificationRepository;
import com.jigmnnaga.support.IntegrationTestSupport;
import java.math.BigDecimal;
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
    private AppointmentConfirmUseCase appointmentConfirmUseCase;

    @Autowired
    private AttendanceRespondUseCase attendanceRespondUseCase;

    @Autowired
    private ReminderSendUseCase reminderSendUseCase;

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

    // ─── 기존 스케줄러 알림 (변경 없음) ─────────────────────────────────────────

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
            LocalDateTime changedDateTime = appointment.getConfirmedDateTime().plusHours(1);
            appointment.confirmDateTime(changedDateTime.toLocalDate(), changedDateTime.toLocalTime());
        });

        scheduledNotificationUseCase.sendPreAppointmentNotifications(now);

        assertThat(received(appointmentId, NotificationType.APPOINTMENT_SOON)).hasSize(3);
    }

    @Test
    @DisplayName("확정된 약속 시간으로부터 24시간이 경과하면 완료 처리되고 completedAt이 기록된다")
    void completeOverdue_confirmedAndExpired() {
        Member host = createMember("태양");
        Member member = createMember("태희");
        Long appointmentId = saveAppointment(host, member, now.minusDays(2), now.minusHours(25), null);

        int count = scheduledNotificationUseCase.completeOverdueAppointments(now);

        assertThat(count).isGreaterThanOrEqualTo(1);
        transactionTemplate.executeWithoutResult(status -> {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
            assertThat(appointment.getCompletedAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("확정된 약속 시간으로부터 24시간이 지나지 않은 약속은 완료 처리되지 않는다")
    void completeOverdue_notYetExpired() {
        Member host = createMember("태양");
        Member member = createMember("태희");
        Long appointmentId = saveAppointment(host, member, now.minusDays(1), now.minusHours(23), null);

        scheduledNotificationUseCase.completeOverdueAppointments(now);

        transactionTemplate.executeWithoutResult(status -> {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
            assertThat(appointment.getCompletedAt()).isNull();
        });
    }

    @Test
    @DisplayName("확정되지 않은 약속은 완료 처리 대상에서 제외된다")
    void completeOverdue_unconfirmedExcluded() {
        Member host = createMember("태양");
        Member member = createMember("태희");
        Long appointmentId = saveAppointment(host, member, now.minusDays(2), null, null);

        scheduledNotificationUseCase.completeOverdueAppointments(now);

        transactionTemplate.executeWithoutResult(status -> {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
            assertThat(appointment.getCompletedAt()).isNull();
        });
    }

    @Test
    @DisplayName("취소된 약속은 완료 처리 대상에서 제외된다")
    void completeOverdue_canceledExcluded() {
        Member host = createMember("태양");
        Member member = createMember("태희");
        Long appointmentId = saveAppointment(host, member, now.minusDays(2), now.minusHours(25), null);

        transactionTemplate.executeWithoutResult(status -> {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
            appointment.cancel();
        });

        scheduledNotificationUseCase.completeOverdueAppointments(now);

        transactionTemplate.executeWithoutResult(status -> {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
            assertThat(appointment.getCompletedAt()).isNull();
        });
    }

    @Test
    @DisplayName("이미 완료된 약속은 중복 처리되지 않는다")
    void completeOverdue_alreadyCompleted_notProcessedAgain() {
        Member host = createMember("태양");
        Member member = createMember("태희");
        Long appointmentId = saveAppointment(host, member, now.minusDays(2), now.minusHours(25), null);

        scheduledNotificationUseCase.completeOverdueAppointments(now);

        LocalDateTime firstCompletedAt = transactionTemplate.execute(status ->
                appointmentRepository.findById(appointmentId).orElseThrow().getCompletedAt());
        assertThat(firstCompletedAt).isNotNull();

        scheduledNotificationUseCase.completeOverdueAppointments(now.plusMinutes(1));

        transactionTemplate.executeWithoutResult(status -> {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
            assertThat(appointment.getCompletedAt()).isEqualTo(firstCompletedAt);
        });
    }

    // ─── 임박 알림 (스케줄러) ────────────────────────────────────────────────

    @Test
    @DisplayName("약속까지 H12 구간(661~720분)인 경우 ATTENDING 미입력 참여자에게 임박 알림을 발송한다")
    void upcoming_H12_sendsToEligibleParticipant() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        // 700분 후 → H12 구간(660 < 700 <= 720)
        Long appointmentId = saveAppointment(host, member, now.minusDays(1),
                now.plusMinutes(700), AttendanceStatus.ATTENDING);

        scheduledNotificationUseCase.sendDepartureInputReminders(now);

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).containsExactly("참여자");
    }

    @Test
    @DisplayName("임박 알림은 동일 단계에 대해 중복 발송되지 않는다")
    void upcoming_deduplication() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        Long appointmentId = saveAppointment(host, member, now.minusDays(1),
                now.plusMinutes(700), AttendanceStatus.ATTENDING);

        scheduledNotificationUseCase.sendDepartureInputReminders(now);
        scheduledNotificationUseCase.sendDepartureInputReminders(now);

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).containsExactly("참여자");
    }

    @Test
    @DisplayName("약속 시각이 이미 지난 경우 임박 알림을 발송하지 않는다")
    void upcoming_pastAppointment_notSent() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        Long appointmentId = saveAppointment(host, member, now.minusDays(2),
                now.minusHours(1), AttendanceStatus.ATTENDING);

        scheduledNotificationUseCase.sendDepartureInputReminders(now);

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).isEmpty();
    }

    @Test
    @DisplayName("미확정 약속은 임박 알림 대상에서 제외된다")
    void upcoming_unconfirmed_excluded() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        Long appointmentId = saveAppointment(host, member, now.plusDays(1), null, null);

        scheduledNotificationUseCase.sendDepartureInputReminders(now);

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).isEmpty();
    }

    @Test
    @DisplayName("취소된 약속은 임박 알림 대상에서 제외된다")
    void upcoming_canceled_excluded() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        Long appointmentId = saveAppointment(host, member, now.minusDays(1),
                now.plusMinutes(700), AttendanceStatus.ATTENDING);

        transactionTemplate.executeWithoutResult(status ->
                appointmentRepository.findById(appointmentId).orElseThrow().cancel());

        scheduledNotificationUseCase.sendDepartureInputReminders(now);

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).isEmpty();
    }

    @Test
    @DisplayName("NOT_ATTENDING 참여자는 임박 알림 대상에서 제외된다")
    void upcoming_notAttending_excluded() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        Long appointmentId = saveAppointment(host, member, now.minusDays(1),
                now.plusMinutes(700), AttendanceStatus.NOT_ATTENDING);

        scheduledNotificationUseCase.sendDepartureInputReminders(now);

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).isEmpty();
    }

    @Test
    @DisplayName("출발 위치와 이동 수단을 모두 입력한 참여자는 임박 알림 대상에서 제외된다")
    void upcoming_completedDepartureInfo_excluded() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        Long appointmentId = saveAppointment(host, member, now.minusDays(1),
                now.plusMinutes(700), AttendanceStatus.ATTENDING);

        transactionTemplate.executeWithoutResult(status -> {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
            AppointmentParticipant participant = appointment.findAcceptedParticipant(member.getId()).orElseThrow();
            participant.registerDepartureLocation("집", new BigDecimal("37.5"), new BigDecimal("127.0"));
            participant.selectTravelMode(TravelMode.SUBWAY, 30, now);
        });

        scheduledNotificationUseCase.sendDepartureInputReminders(now);

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).isEmpty();
    }

    @Test
    @DisplayName("출발 위치만 입력(이동 수단 미입력)한 참여자는 임박 알림 대상에 포함된다")
    void upcoming_locationOnlyWithoutTravelMode_eligible() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        Long appointmentId = saveAppointment(host, member, now.minusDays(1),
                now.plusMinutes(700), AttendanceStatus.ATTENDING);

        transactionTemplate.executeWithoutResult(status -> {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
            appointment.findAcceptedParticipant(member.getId()).orElseThrow()
                    .registerDepartureLocation("집", new BigDecimal("37.5"), new BigDecimal("127.0"));
        });

        scheduledNotificationUseCase.sendDepartureInputReminders(now);

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).containsExactly("참여자");
    }

    @Test
    @DisplayName("약속 시각이 24시간 초과인 약속은 임박 알림 범위가 아니므로 발송되지 않는다")
    void upcoming_beyond24h_notSentByUpcoming() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        // 14시 기준으로 1500분(25시간) 후 → 임박 최대 범위(24h=1440분) 초과
        // 10시/18시 슬롯이 아니므로 정기 알림도 발송되지 않는다
        LocalDateTime offSlotNow = now.withHour(14).withMinute(0).withSecond(0);
        Long appointmentId = saveAppointment(host, member, offSlotNow.minusDays(1),
                offSlotNow.plusMinutes(1500), AttendanceStatus.ATTENDING);

        scheduledNotificationUseCase.sendDepartureInputReminders(offSlotNow);

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).isEmpty();
    }

    // ─── 정기 알림 (스케줄러) ────────────────────────────────────────────────

    @Test
    @DisplayName("AM 10시 슬롯에서 24시간 초과 약속의 미입력 참여자에게 정기 알림을 발송한다")
    void periodic_AM_sendsAtHour10() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        LocalDateTime periodicNow = now.withHour(10).withMinute(0).withSecond(0);
        Long appointmentId = saveAppointment(host, member, periodicNow.minusDays(1),
                periodicNow.plusHours(30), AttendanceStatus.ATTENDING);

        scheduledNotificationUseCase.sendDepartureInputReminders(periodicNow);

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).containsExactly("참여자");
    }

    @Test
    @DisplayName("PM 18시 슬롯에서 24시간 초과 약속의 미입력 참여자에게 정기 알림을 발송한다")
    void periodic_PM_sendsAtHour18() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        LocalDateTime periodicNow = now.withHour(18).withMinute(0).withSecond(0);
        Long appointmentId = saveAppointment(host, member, periodicNow.minusDays(1),
                periodicNow.plusHours(30), AttendanceStatus.ATTENDING);

        scheduledNotificationUseCase.sendDepartureInputReminders(periodicNow);

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).containsExactly("참여자");
    }

    @Test
    @DisplayName("10시/18시가 아닌 시각에는 정기 알림을 발송하지 않는다")
    void periodic_notAtSlot_notSent() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        LocalDateTime offSlotNow = now.withHour(14).withMinute(0).withSecond(0);
        Long appointmentId = saveAppointment(host, member, offSlotNow.minusDays(1),
                offSlotNow.plusHours(30), AttendanceStatus.ATTENDING);

        scheduledNotificationUseCase.sendDepartureInputReminders(offSlotNow);

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).isEmpty();
    }

    @Test
    @DisplayName("약속까지 24시간 이내인 경우 정기 알림 범위가 아니므로 발송되지 않는다")
    void periodic_within24h_notSentByPeriodic() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        LocalDateTime periodicNow = now.withHour(10).withMinute(0).withSecond(0);
        // 12시간 후 → 24시간 이내 → 정기 알림 범위 아님 (임박 H12 구간)
        Long appointmentId = saveAppointment(host, member, periodicNow.minusDays(1),
                periodicNow.plusHours(12), AttendanceStatus.ATTENDING);

        // H12 임박 알림 이력을 미리 기록해 임박 알림 중복 방지
        transactionTemplate.executeWithoutResult(status -> {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
            appointment.findAcceptedParticipant(member.getId()).orElseThrow()
                    .markDepartureNotifUpcomingSent(DepartureUpcomingStage.H12, periodicNow);
        });

        scheduledNotificationUseCase.sendDepartureInputReminders(periodicNow);

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).isEmpty();
    }

    @Test
    @DisplayName("AM 슬롯 오늘 이미 발송된 참여자는 같은 날 같은 슬롯에서 중복 발송되지 않는다")
    void periodic_AM_dailyDedup() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        LocalDateTime periodicNow = now.withHour(10).withMinute(0).withSecond(0);
        Long appointmentId = saveAppointment(host, member, periodicNow.minusDays(1),
                periodicNow.plusHours(30), AttendanceStatus.ATTENDING);

        scheduledNotificationUseCase.sendDepartureInputReminders(periodicNow);
        scheduledNotificationUseCase.sendDepartureInputReminders(periodicNow.plusMinutes(1));

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).containsExactly("참여자");
    }

    @Test
    @DisplayName("직전 초기/정기 알림으로부터 4시간이 경과하지 않은 경우 정기 알림을 발송하지 않는다")
    void periodic_fourHourGap_notSatisfied() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        LocalDateTime periodicNow = now.withHour(18).withMinute(0).withSecond(0);
        Long appointmentId = saveAppointment(host, member, periodicNow.minusDays(1),
                periodicNow.plusHours(30), AttendanceStatus.ATTENDING);

        // 최초 알림을 2시간 전에 발송한 것으로 기록
        transactionTemplate.executeWithoutResult(status -> {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
            appointment.findAcceptedParticipant(member.getId()).orElseThrow()
                    .markDepartureNotifInitialSent(periodicNow.minusHours(2));
        });

        scheduledNotificationUseCase.sendDepartureInputReminders(periodicNow);

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).isEmpty();
    }

    @Test
    @DisplayName("자동 알림이 발송된 이후에도 방장의 수동 리마인더는 정상 발송된다")
    void departureInputReminder_manualReminderStillWorks() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        Long appointmentId = saveAppointment(host, member, now.minusDays(1), now.plusMinutes(700),
                AttendanceStatus.ATTENDING);

        scheduledNotificationUseCase.sendDepartureInputReminders(now);
        reminderSendUseCase.sendReminder(host.getId(), appointmentId);

        // 자동 1회 + 수동 1회 = 2회
        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).hasSize(2);
    }

    // ─── 약속 확정 트리거 ────────────────────────────────────────────────────

    @Test
    @DisplayName("약속 확정 직후 약속 시각이 미래이면 24시간 제한 없이 ATTENDING 참여자에게 최초 알림을 발송한다")
    void confirmationTrigger_sendsRegardlessOf24hWindow() {
        Member host = createMember("방장");
        Member member = createMember("참여자");

        Long[] locationIdHolder = new Long[1];
        Long appointmentId = transactionTemplate.execute(status -> {
            LocalDateTime confirmedAt = now.plusHours(30); // 24시간 초과
            Appointment appointment = Appointment.builder()
                    .title("확정 트리거 테스트")
                    .dateSelectionType(DateSelectionType.SINGLE)
                    .responseDeadline(now.minusDays(1))
                    .inviteCode("confirm-trigger-" + System.nanoTime())
                    .build();
            appointment.addCandidateDate(AppointmentCandidateDate.builder()
                    .candidateDate(confirmedAt.toLocalDate()).build());
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
            appointment.confirmDateTime(confirmedAt.toLocalDate(), confirmedAt.toLocalTime());
            appointment.findAcceptedParticipant(member.getId()).orElseThrow()
                    .respondAttendance(AttendanceStatus.ATTENDING);
            locationIdHolder[0] = appointment.getCandidateLocations().getFirst().getId();
            return appointment.getId();
        });

        // confirmLocation 호출 → 최초 알림 트리거
        appointmentConfirmUseCase.confirmLocation(host.getId(), appointmentId,
                new ConfirmLocationRequest(locationIdHolder[0],
                        new DetailPlaceRequest("스터디카페", "서울 강남구", 37.5, 127.0)));

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).containsExactly("참여자");
    }

    @Test
    @DisplayName("약속 확정 트리거로 최초 알림이 발송된 경우 재확정 시 중복 발송되지 않는다")
    void confirmationTrigger_deduplication() {
        Member host = createMember("방장");
        Member member = createMember("참여자");

        Long[] locationIdHolder = new Long[1];
        Long appointmentId = transactionTemplate.execute(status -> {
            LocalDateTime confirmedAt = now.plusHours(30);
            Appointment appointment = Appointment.builder()
                    .title("확정 중복 방지 테스트")
                    .dateSelectionType(DateSelectionType.SINGLE)
                    .responseDeadline(now.minusDays(1))
                    .inviteCode("confirm-dedup-" + System.nanoTime())
                    .build();
            appointment.addCandidateDate(AppointmentCandidateDate.builder()
                    .candidateDate(confirmedAt.toLocalDate()).build());
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
            appointment.confirmDateTime(confirmedAt.toLocalDate(), confirmedAt.toLocalTime());
            appointment.findAcceptedParticipant(member.getId()).orElseThrow()
                    .respondAttendance(AttendanceStatus.ATTENDING);
            locationIdHolder[0] = appointment.getCandidateLocations().getFirst().getId();
            return appointment.getId();
        });

        // 첫 번째 확정 → 최초 알림 1회 발송
        appointmentConfirmUseCase.confirmLocation(host.getId(), appointmentId,
                new ConfirmLocationRequest(locationIdHolder[0],
                        new DetailPlaceRequest("스터디카페A", "서울 강남구", 37.5, 127.0)));
        // 두 번째 확정 (장소명 변경) → 최초 알림 중복 발송 안 됨
        appointmentConfirmUseCase.confirmLocation(host.getId(), appointmentId,
                new ConfirmLocationRequest(locationIdHolder[0],
                        new DetailPlaceRequest("스터디카페B", "서울 강남구", 37.5, 127.0)));

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).containsExactly("참여자");
    }

    // ─── 출석 응답 트리거 ────────────────────────────────────────────────────

    @Test
    @DisplayName("참석(ATTENDING) 응답 시 출발 정보 미입력 참여자에게 최초 알림을 발송한다")
    void attendanceTrigger_sendsInitialWhenAttending() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        // 약속 확정, 출석 응답 없음(PENDING)
        Long appointmentId = saveAppointment(host, member, now.minusDays(1),
                now.plusHours(30), null);

        // ATTENDING 응답 → 최초 알림 트리거
        attendanceRespondUseCase.respondAttendance(member.getId(), appointmentId,
                new AttendanceRespondRequest(AttendanceStatus.ATTENDING));

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).containsExactly("참여자");
    }

    @Test
    @DisplayName("참석 응답으로 최초 알림이 발송된 이후 다시 ATTENDING으로 응답해도 중복 발송되지 않는다")
    void attendanceTrigger_notSentIfAlreadyInitialSent() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        Long appointmentId = saveAppointment(host, member, now.minusDays(1),
                now.plusHours(30), null);

        // 첫 번째 ATTENDING → 최초 알림 발송
        attendanceRespondUseCase.respondAttendance(member.getId(), appointmentId,
                new AttendanceRespondRequest(AttendanceStatus.ATTENDING));
        // RESCHEDULE_REQUESTED → 다시 ATTENDING → 최초 알림 중복 발송 안 됨
        attendanceRespondUseCase.respondAttendance(member.getId(), appointmentId,
                new AttendanceRespondRequest(AttendanceStatus.RESCHEDULE_REQUESTED));
        attendanceRespondUseCase.respondAttendance(member.getId(), appointmentId,
                new AttendanceRespondRequest(AttendanceStatus.ATTENDING));

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).containsExactly("참여자");
    }

    // ─── 약속 시각 변경 시 임박 알림 이력 초기화 ─────────────────────────────

    @Test
    @DisplayName("약속 시각 변경 시 임박 알림 이력이 초기화되어 변경 후 임박 구간에서 재발송된다")
    void datetimeReset_upcomingNotifHistoryCleared() {
        Member host = createMember("방장");
        Member member = createMember("참여자");
        // 700분 후 → H12 구간
        Long appointmentId = saveAppointment(host, member, now.minusDays(1),
                now.plusMinutes(700), AttendanceStatus.ATTENDING);

        // H12 임박 알림 발송
        scheduledNotificationUseCase.sendDepartureInputReminders(now);
        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).containsExactly("참여자");

        // 약속 시각 변경 → 임박 알림 이력 및 출석 상태 초기화
        transactionTemplate.executeWithoutResult(status -> {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
            LocalDateTime newConfirmedAt = now.plusMinutes(710);
            appointment.confirmDateTime(newConfirmedAt.toLocalDate(), newConfirmedAt.toLocalTime());
            // confirmDateTime은 출석 상태를 PENDING으로 초기화하므로 재참석 응답 필요
            appointment.findAcceptedParticipant(member.getId()).orElseThrow()
                    .respondAttendance(AttendanceStatus.ATTENDING);
        });

        // 이력 초기화 후 같은 H12 구간에서 재발송
        scheduledNotificationUseCase.sendDepartureInputReminders(now);

        assertThat(received(appointmentId, NotificationType.DEPARTURE_INPUT_REQUESTED)).hasSize(2);
    }
}
