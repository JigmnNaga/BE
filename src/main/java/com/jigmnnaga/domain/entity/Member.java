package com.jigmnnaga.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    private static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long kakaoId;

    @Column(nullable = false)
    private String nickname;

    private String profileImageUrl;

    private String profileImageKey;

    private String defaultLocationName;

    private String defaultLocationAddress;

    @Column(precision = 10, scale = 7)
    private BigDecimal defaultLatitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal defaultLongitude;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private LocalDateTime withdrawnAt;

    @Builder
    private Member(Long kakaoId, String nickname, String profileImageUrl, String email, Role role) {
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.email = email;
        this.role = role;
    }

    public void withdraw(LocalDateTime withdrawnAt) {
        this.kakaoId = -id;
        this.nickname = WITHDRAWN_NICKNAME;
        removeProfileImage();
        clearDefaultLocation();
        this.email = null;
        this.withdrawnAt = withdrawnAt;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeProfileImage(String profileImageKey) {
        this.profileImageKey = profileImageKey;
        this.profileImageUrl = null;
    }

    public void removeProfileImage() {
        this.profileImageKey = null;
        this.profileImageUrl = null;
    }

    public void updateDefaultLocation(String name, String address, BigDecimal latitude, BigDecimal longitude) {
        this.defaultLocationName = name;
        this.defaultLocationAddress = address;
        this.defaultLatitude = latitude;
        this.defaultLongitude = longitude;
    }

    public void clearDefaultLocation() {
        updateDefaultLocation(null, null, null, null);
    }

    public boolean hasDefaultLocation() {
        return defaultLatitude != null && defaultLongitude != null;
    }

    public boolean isWithdrawn() {
        return withdrawnAt != null;
    }
}
