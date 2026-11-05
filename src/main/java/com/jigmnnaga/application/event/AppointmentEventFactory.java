package com.jigmnnaga.application.event;

import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.NotificationType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class AppointmentEventFactory {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("M월 d일(E) a h:mm", Locale.KOREAN);
    private static final String SEPARATOR = " · ";

    public NotificationEvent invited(Appointment appointment, String hostNickname, List<Long> recipientIds) {
        return new NotificationEvent(recipientIds, appointment.getId(), NotificationType.INVITED,
                "약속에 초대됐어요",
                hostNickname + "님이 '" + appointment.getTitle() + "' 약속에 초대했어요. 가능한 일정과 장소를 선택해 주세요.");
    }

    public NotificationEvent scheduleInputRequested(Appointment appointment, List<Long> recipientIds) {
        return new NotificationEvent(recipientIds, appointment.getId(), NotificationType.SCHEDULE_INPUT_REQUESTED,
                "가능한 일정을 알려주세요",
                "'" + appointment.getTitle() + "' 약속의 가능한 시간과 희망 장소를 선택해 주세요.");
    }

    public NotificationEvent confirmed(Appointment appointment, List<Long> recipientIds) {
        return new NotificationEvent(recipientIds, appointment.getId(), NotificationType.APPOINTMENT_CONFIRMED,
                "약속이 확정됐어요",
                "'" + appointment.getTitle() + "' " + confirmedSummary(appointment));
    }

    public NotificationEvent confirmedChanged(Appointment appointment, List<Long> recipientIds) {
        return new NotificationEvent(recipientIds, appointment.getId(), NotificationType.APPOINTMENT_CHANGED,
                "확정된 약속이 변경됐어요",
                "'" + appointment.getTitle() + "' " + confirmedSummary(appointment) + "(으)로 변경됐어요.");
    }

    public NotificationEvent infoChanged(Appointment appointment, String change, List<Long> recipientIds) {
        return new NotificationEvent(recipientIds, appointment.getId(), NotificationType.APPOINTMENT_CHANGED,
                "약속 정보가 변경됐어요",
                "'" + appointment.getTitle() + "' 약속의 " + change);
    }

    public NotificationEvent attendanceRequested(Appointment appointment, List<Long> recipientIds) {
        return new NotificationEvent(recipientIds, appointment.getId(), NotificationType.ATTENDANCE_REQUESTED,
                "참석 여부를 알려주세요",
                "'" + appointment.getTitle() + "' 약속에 참석할 수 있는지 응답해 주세요.");
    }

    public NotificationEvent canceled(Appointment appointment, List<Long> recipientIds) {
        return new NotificationEvent(recipientIds, appointment.getId(), NotificationType.APPOINTMENT_CANCELED,
                "약속이 취소됐어요",
                "'" + appointment.getTitle() + "' 약속이 취소됐어요.");
    }

    public NotificationEvent hostDelegated(Appointment appointment, Long newHostId) {
        return new NotificationEvent(List.of(newHostId), appointment.getId(), NotificationType.HOST_DELEGATED,
                "방장이 되었어요",
                "'" + appointment.getTitle() + "' 약속의 방장을 위임받았어요.");
    }

    public NotificationEvent kicked(Appointment appointment, Long kickedMemberId) {
        return new NotificationEvent(List.of(kickedMemberId), appointment.getId(), NotificationType.KICKED,
                "약속방에서 내보내졌어요",
                "'" + appointment.getTitle() + "' 약속방에서 내보내졌어요.");
    }

    public String confirmedChatMessage(Appointment appointment) {
        return "약속이 확정됐어요" + SEPARATOR + confirmedSummary(appointment);
    }

    public String confirmedChangedChatMessage(Appointment appointment) {
        return "약속이 변경됐어요" + SEPARATOR + confirmedSummary(appointment);
    }

    public String canceledChatMessage() {
        return "약속이 취소됐어요";
    }

    public String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    private String confirmedSummary(Appointment appointment) {
        String place = appointment.getConfirmedLocation().getPlaceName();
        if (appointment.getDetailPlaceName() != null) {
            place += " " + appointment.getDetailPlaceName();
        }
        return formatDateTime(appointment.getConfirmedDateTime()) + SEPARATOR + place;
    }
}
