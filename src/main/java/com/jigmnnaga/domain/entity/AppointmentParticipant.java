package com.jigmnnaga.domain.entity;

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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

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
