package com.jigmnnaga.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "participant_availability")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantAvailability extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private AppointmentParticipant participant;

    @Column(nullable = false)
    private LocalDate availableDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Builder
    private ParticipantAvailability(LocalDate availableDate, LocalTime startTime, LocalTime endTime) {
        this.availableDate = availableDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    void assignParticipant(AppointmentParticipant participant) {
        this.participant = participant;
    }
}
