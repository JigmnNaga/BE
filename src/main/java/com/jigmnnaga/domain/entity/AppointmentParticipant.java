
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
import java.math.BigDecimal; // 추가
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // 추가: 출발 위치 정보
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

    public void updateSchedule(List<ParticipantAvailability> newAvailabilities,
                               AppointmentCandidateLocation preferredLocation) {
        availabilities.clear();
        newAvailabilities.forEach(availability -> {
            availabilities.add(availability);
            availability.assignParticipant(this);
        });
        this.preferredLocation = preferredLocation;
    }

    public void selectTravelMode(TravelMode travelMode, Integer travelDurationMinutes, LocalDateTime selectedAt) {
        this.travelMode = travelMode;
        this.travelDurationMinutes = travelDurationMinutes;
        this.travelModeSelectedAt = selectedAt;
    }

    // 추가: 출발 위치 등록
    public void registerDepartureLocation(
            String name,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        this.departureLocationName = name;
        this.departureLatitude = latitude;
        this.departureLongitude = longitude;
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

    public void arrive() {
        if (departureStatus == DepartureStatus.BEFORE_DEPARTURE) {
            throw new BusinessException(ErrorCode.NOT_YET_DEPARTED);
        }
        if (departureStatus == DepartureStatus.ARRIVED) {
            throw new BusinessException(ErrorCode.ALREADY_ARRIVED);
        }
        this.departureStatus = DepartureStatus.ARRIVED;
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
        return !availabilities.isEmpty();
    }

    public boolean hasDepartureLocation() {
        return departureLatitude != null && departureLongitude != null;
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