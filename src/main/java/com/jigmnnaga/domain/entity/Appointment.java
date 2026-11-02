package com.jigmnnaga.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
