
package com.jigmnnaga.domain.entity;

import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "appointment_participant",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_appointment_participant_appointment_member",
                columnNames = {"appointment_id", "member_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppointmentParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationStatus status;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING'")
    @Column(nullable = false)
    private AttendanceStatus attendanceStatus = AttendanceStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_location_id")
    private AppointmentCandidateLocation preferredLocation;

    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParticipantAvailability> availabilities = new ArrayList<>();

    private Long lastReadMessageId;

    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean notificationEnabled = true;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private TravelMode travelMode;

    private Integer travelDurationMinutes;

    private LocalDateTime travelModeSelectedAt;

    @Column(name = "departure_location_name", length = 255)
    private String departureLocationName;

    @Column(name = "departure_latitude", precision = 10, scale = 7)
    private BigDecimal departureLatitude;

    @Column(name = "departure_longitude", precision = 10, scale = 7)
    private BigDecimal departureLongitude;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @ColumnDefault("'BEFORE_DEPARTURE'")
    @Column(name = "departure_status", nullable = false, length = 30)
    private DepartureStatus departureStatus = DepartureStatus.BEFORE_DEPARTURE;

    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean locationSharingEnabled = true;

    private LocalDateTime arrivedAt;

    // 출발 시간 알림 이력 (참여자 단위)
    private LocalDateTime departurePrepareNotifSentAt;
    private LocalDateTime departureStartTimeNotifSentAt;

    // 출발 정보 입력 요청 자동 알림 이력 (참여자 단위)
    private LocalDateTime departureNotifInitialSentAt;
    private LocalDateTime departureNotifPeriodicAmSentAt;
    private LocalDateTime departureNotifPeriodicPmSentAt;
    @Column(name = "departure_notif_upcoming_24h_sent_at")
    private LocalDateTime departureNotifUpcoming24hSentAt;
    @Column(name = "departure_notif_upcoming_12h_sent_at")
    private LocalDateTime departureNotifUpcoming12hSentAt;
    @Column(name = "departure_notif_upcoming_6h_sent_at")
    private LocalDateTime departureNotifUpcoming6hSentAt;
    @Column(name = "departure_notif_upcoming_3h_sent_at")
    private LocalDateTime departureNotifUpcoming3hSentAt;
    @Column(name = "departure_notif_upcoming_1h_sent_at")
    private LocalDateTime departureNotifUpcoming1hSentAt;

    @Builder
    private AppointmentParticipant(Member member, ParticipantRole role, ParticipationStatus status) {
        this.member = member;
        this.role = role;
        this.status = status;
    }

    public void accept() {
        this.status = ParticipationStatus.ACCEPTED;
    }

    public void decline() {
        this.status = ParticipationStatus.DECLINED;
    }

    public void updateAvailabilities(List<ParticipantAvailability> newAvailabilities) {
        availabilities.clear();
        newAvailabilities.forEach(availability -> {
            availabilities.add(availability);
            availability.assignParticipant(this);
        });
    }

    public void selectPreferredLocation(AppointmentCandidateLocation preferredLocation) {
        this.preferredLocation = preferredLocation;
    }

    public void selectTravelMode(TravelMode travelMode, Integer travelDurationMinutes, LocalDateTime selectedAt) {
        this.travelMode = travelMode;
        this.travelDurationMinutes = travelDurationMinutes;
        this.travelModeSelectedAt = selectedAt;
    }

    public void registerDepartureLocation(String name, BigDecimal latitude, BigDecimal longitude) {
        if (departureStatus != DepartureStatus.BEFORE_DEPARTURE) {
            throw new BusinessException(ErrorCode.DEPARTURE_LOCATION_LOCKED);
        }
        boolean locationChanged = isDepartureLocationChanged(latitude, longitude);
        this.departureLocationName = name;
        this.departureLatitude = latitude;
        this.departureLongitude = longitude;
        if (locationChanged) {
            this.travelMode = null;
            this.travelDurationMinutes = null;
        }
    }

    private static final int COORDINATE_SCALE = 7;

    private boolean isDepartureLocationChanged(BigDecimal newLatitude, BigDecimal newLongitude) {
        if (this.departureLatitude == null || this.departureLongitude == null) {
            return false;
        }
        BigDecimal existingLat = this.departureLatitude.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
        BigDecimal existingLon = this.departureLongitude.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
        BigDecimal incomingLat = newLatitude.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
        BigDecimal incomingLon = newLongitude.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
        return existingLat.compareTo(incomingLat) != 0 || existingLon.compareTo(incomingLon) != 0;
    }

    public void depart() {
        if (departureStatus == DepartureStatus.DEPARTED) {
            throw new BusinessException(ErrorCode.ALREADY_DEPARTED);
        }
        if (departureStatus == DepartureStatus.ARRIVED) {
            throw new BusinessException(ErrorCode.ALREADY_ARRIVED);
        }
        this.departureStatus = DepartureStatus.DEPARTED;
    }

    public void arrive(LocalDateTime arrivedAt) {
        if (departureStatus == DepartureStatus.BEFORE_DEPARTURE) {
            throw new BusinessException(ErrorCode.NOT_YET_DEPARTED);
        }
        if (departureStatus == DepartureStatus.ARRIVED) {
            throw new BusinessException(ErrorCode.ALREADY_ARRIVED);
        }
        this.departureStatus = DepartureStatus.ARRIVED;
        this.arrivedAt = arrivedAt;
    }

    public boolean arrivedOnTime() {
        LocalDateTime appointmentDateTime = appointment.getConfirmedDateTime();
        return arrivedAt != null && appointmentDateTime != null
                && !arrivedAt.truncatedTo(ChronoUnit.MINUTES).isAfter(appointmentDateTime);
    }

    public void stopLocationSharing() {
        this.locationSharingEnabled = false;
    }

    public void resumeLocationSharing() {
        if (departureStatus == DepartureStatus.ARRIVED) {
            throw new BusinessException(ErrorCode.ALREADY_ARRIVED);
        }
        this.locationSharingEnabled = true;
    }

    public void updateNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    public boolean readUpTo(Long messageId) {
        if (messageId == null || (lastReadMessageId != null && lastReadMessageId >= messageId)) {
            return false;
        }
        this.lastReadMessageId = messageId;
        return true;
    }

    public boolean hasRead(Long messageId) {
        return lastReadMessageId != null && lastReadMessageId >= messageId;
    }

    public void kick() {
        this.status = ParticipationStatus.KICKED;
        availabilities.clear();
        this.preferredLocation = null;
        resetAttendance();
    }

    public void promoteToHost() {
        this.role = ParticipantRole.HOST;
    }

    public void demoteToMember() {
        this.role = ParticipantRole.MEMBER;
    }

    public boolean isKicked() {
        return status == ParticipationStatus.KICKED;
    }

    public void removeAvailabilitiesNotIn(Set<LocalDate> candidateDates) {
        availabilities.removeIf(availability -> !candidateDates.contains(availability.getAvailableDate()));
    }

    public void clearPreferredLocationIfIn(List<AppointmentCandidateLocation> locations) {
        if (locations.contains(preferredLocation)) {
            this.preferredLocation = null;
        }
    }

    public void respondAttendance(AttendanceStatus attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public void resetAttendance() {
        this.attendanceStatus = AttendanceStatus.PENDING;
    }

    public boolean hasSubmittedSchedule() {
        return !availabilities.isEmpty() && preferredLocation != null;
    }

    public boolean hasDepartureLocation() {
        return departureLatitude != null && departureLongitude != null;
    }

    /** 출발 정보 입력 완료 여부: 출발 위치 + 이동 수단 모두 입력 시 완료 */
    public boolean hasCompletedDepartureInfo() {
        return departureLatitude != null && departureLongitude != null && travelMode != null;
    }

    /** 자동 출발 정보 입력 요청 알림 발송 대상 여부 */
    public boolean isEligibleForDepartureAutoNotif() {
        return status == ParticipationStatus.ACCEPTED
                && attendanceStatus == AttendanceStatus.ATTENDING
                && !hasCompletedDepartureInfo();
    }

    /**
     * 출발 시간 알림(준비 알림·출발 알림) 발송 대상 여부.
     * 출발 위치·이동 수단 완료 + travelDurationMinutes 양수 + 아직 출발 전 조건을 모두 만족해야 한다.
     */
    public boolean isEligibleForDepartureTimeNotif() {
        return status == ParticipationStatus.ACCEPTED
                && attendanceStatus == AttendanceStatus.ATTENDING
                && hasCompletedDepartureInfo()
                && travelDurationMinutes != null
                && travelDurationMinutes > 0
                && departureStatus == DepartureStatus.BEFORE_DEPARTURE;
    }

    // ─── 출발 시간 알림 ───────────────────────────────────────────────────────

    public boolean isDeparturePrepareNotifSent() {
        return departurePrepareNotifSentAt != null;
    }

    public void markDeparturePrepareNotifSent(LocalDateTime sentAt) {
        this.departurePrepareNotifSentAt = sentAt;
    }

    public boolean isDepartureStartTimeNotifSent() {
        return departureStartTimeNotifSentAt != null;
    }

    public void markDepartureStartTimeNotifSent(LocalDateTime sentAt) {
        this.departureStartTimeNotifSentAt = sentAt;
    }

    /** 약속 날짜/시간 변경 시 출발 시간 알림 이력 초기화 */
    public void resetDepartureTimeNotifHistory() {
        this.departurePrepareNotifSentAt = null;
        this.departureStartTimeNotifSentAt = null;
    }

    // ─── 최초 알림 ────────────────────────────────────────────────────────────

    public boolean isDepartureNotifInitialSent() {
        return departureNotifInitialSentAt != null;
    }

    public void markDepartureNotifInitialSent(LocalDateTime sentAt) {
        this.departureNotifInitialSentAt = sentAt;
    }

    // ─── 정기 알림 (AM 10시 / PM 18시) ───────────────────────────────────────

    public boolean isDepartureNotifPeriodicAmSentToday(LocalDate today) {
        return departureNotifPeriodicAmSentAt != null
                && departureNotifPeriodicAmSentAt.toLocalDate().equals(today);
    }

    public void markDepartureNotifPeriodicAmSent(LocalDateTime sentAt) {
        this.departureNotifPeriodicAmSentAt = sentAt;
    }

    public boolean isDepartureNotifPeriodicPmSentToday(LocalDate today) {
        return departureNotifPeriodicPmSentAt != null
                && departureNotifPeriodicPmSentAt.toLocalDate().equals(today);
    }

    public void markDepartureNotifPeriodicPmSent(LocalDateTime sentAt) {
        this.departureNotifPeriodicPmSentAt = sentAt;
    }

    /** 최초 알림 또는 직전 정기 알림 이후 4시간 경과 여부 */
    public boolean isDeparturePeriodicGapSatisfied(LocalDateTime now) {
        LocalDateTime last = getDepartureNotifLastInitialOrPeriodicSentAt();
        return last == null || !last.plusHours(4).isAfter(now);
    }

    private LocalDateTime getDepartureNotifLastInitialOrPeriodicSentAt() {
        LocalDateTime max = null;
        for (LocalDateTime t : new LocalDateTime[]{
                departureNotifInitialSentAt,
                departureNotifPeriodicAmSentAt,
                departureNotifPeriodicPmSentAt}) {
            if (t != null && (max == null || t.isAfter(max))) {
                max = t;
            }
        }
        return max;
    }

    // ─── 임박 알림 ────────────────────────────────────────────────────────────

    public boolean isDepartureNotifUpcomingSent(DepartureUpcomingStage stage) {
        return switch (stage) {
            case H24 -> departureNotifUpcoming24hSentAt != null;
            case H12 -> departureNotifUpcoming12hSentAt != null;
            case H6 -> departureNotifUpcoming6hSentAt != null;
            case H3 -> departureNotifUpcoming3hSentAt != null;
            case H1 -> departureNotifUpcoming1hSentAt != null;
        };
    }

    public void markDepartureNotifUpcomingSent(DepartureUpcomingStage stage, LocalDateTime sentAt) {
        switch (stage) {
            case H24 -> this.departureNotifUpcoming24hSentAt = sentAt;
            case H12 -> this.departureNotifUpcoming12hSentAt = sentAt;
            case H6 -> this.departureNotifUpcoming6hSentAt = sentAt;
            case H3 -> this.departureNotifUpcoming3hSentAt = sentAt;
            case H1 -> this.departureNotifUpcoming1hSentAt = sentAt;
        }
    }

    /** 약속 날짜/시간 변경 시 임박 알림 이력 초기화 */
    public void resetDepartureUpcomingNotifHistory() {
        this.departureNotifUpcoming24hSentAt = null;
        this.departureNotifUpcoming12hSentAt = null;
        this.departureNotifUpcoming6hSentAt = null;
        this.departureNotifUpcoming3hSentAt = null;
        this.departureNotifUpcoming1hSentAt = null;
    }

    public boolean isHost() {
        return role == ParticipantRole.HOST;
    }

    public boolean hasResponded() {
        return status != ParticipationStatus.PENDING;
    }

    void assignAppointment(Appointment appointment) {
        this.appointment = appointment;
    }
}
