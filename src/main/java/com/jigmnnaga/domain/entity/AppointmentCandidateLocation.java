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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "appointment_candidate_location")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppointmentCandidateLocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(nullable = false)
    private String placeName;

    private String address;

    private String lineNames;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Builder
    private AppointmentCandidateLocation(String placeName, String address, String lineNames,
                                         Double latitude, Double longitude) {
        this.placeName = placeName;
        this.address = address;
        this.lineNames = lineNames;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    void assignAppointment(Appointment appointment) {
        this.appointment = appointment;
    }
}
