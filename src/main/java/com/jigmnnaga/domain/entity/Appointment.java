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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "appointment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Appointment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DateSelectionType dateSelectionType;

    @Column(nullable = false)
    private LocalDateTime responseDeadline;

    @Column(nullable = false, unique = true)
    private String inviteCode;

    private LocalDate confirmedDate;

    private LocalTime confirmedTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_location_id")
    private AppointmentCandidateLocation confirmedLocation;

    private String detailPlaceName;

    private String detailPlaceAddress;

    private Double detailPlaceLatitude;

    private Double detailPlaceLongitude;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AppointmentCandidateDate> candidateDates = new ArrayList<>();

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AppointmentCandidateLocation> candidateLocations = new ArrayList<>();

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AppointmentParticipant> participants = new ArrayList<>();

    @Builder
    private Appointment(String title, DateSelectionType dateSelectionType,
                        LocalDateTime responseDeadline, String inviteCode) {
        this.title = title;
        this.dateSelectionType = dateSelectionType;
        this.responseDeadline = responseDeadline;
        this.inviteCode = inviteCode;
    }

    public void confirmDateTime(LocalDate confirmedDate, LocalTime confirmedTime) {
        boolean isChanged = !confirmedDate.equals(this.confirmedDate) || !confirmedTime.equals(this.confirmedTime);
        if (isChanged) {
            participants.forEach(AppointmentParticipant::resetAttendance);
        }
        this.confirmedDate = confirmedDate;
        this.confirmedTime = confirmedTime;
    }

    public void confirmLocation(AppointmentCandidateLocation confirmedLocation, String detailPlaceName,
                                String detailPlaceAddress, Double detailPlaceLatitude, Double detailPlaceLongitude) {
        this.confirmedLocation = confirmedLocation;
        this.detailPlaceName = detailPlaceName;
        this.detailPlaceAddress = detailPlaceAddress;
        this.detailPlaceLatitude = detailPlaceLatitude;
        this.detailPlaceLongitude = detailPlaceLongitude;
    }

    public boolean isConfirmed() {
        return confirmedDate != null && confirmedLocation != null;
    }

    public boolean isUpcoming(LocalDateTime now) {
        if (confirmedDate != null) {
            return !now.isAfter(getConfirmedDateTime());
        }
        return !getLastCandidateDate().isBefore(now.toLocalDate());
    }

    public LocalDateTime getConfirmedDateTime() {
        return confirmedDate == null ? null : confirmedDate.atTime(confirmedTime);
    }

    public LocalDateTime getScheduledDateTime() {
        return confirmedDate == null ? getFirstCandidateDate().atStartOfDay() : getConfirmedDateTime();
    }

    public LocalDate getFirstCandidateDate() {
        return candidateDates.stream()
                .map(AppointmentCandidateDate::getCandidateDate)
                .min(LocalDate::compareTo)
                .orElseThrow();
    }

    public LocalDate getLastCandidateDate() {
        return candidateDates.stream()
                .map(AppointmentCandidateDate::getCandidateDate)
                .max(LocalDate::compareTo)
                .orElseThrow();
    }

    public int getAcceptedMemberCount() {
        return (int) participants.stream()
                .filter(participant -> participant.getStatus() == ParticipationStatus.ACCEPTED)
                .count();
    }

    public boolean isAppointmentTimePassed(LocalDateTime now) {
        return isConfirmed() && !now.isBefore(getConfirmedDateTime());
    }

    public boolean isInviteExpired(LocalDateTime now) {
        return !now.isBefore(responseDeadline);
    }

    public void addCandidateDate(AppointmentCandidateDate candidateDate) {
        candidateDates.add(candidateDate);
        candidateDate.assignAppointment(this);
    }

    public void addCandidateLocation(AppointmentCandidateLocation candidateLocation) {
        candidateLocations.add(candidateLocation);
        candidateLocation.assignAppointment(this);
    }

    public void addParticipant(AppointmentParticipant participant) {
        participants.add(participant);
        participant.assignAppointment(this);
    }
}
