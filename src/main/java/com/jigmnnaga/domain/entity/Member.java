package com.jigmnnaga.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "member", uniqueConstraints = @UniqueConstraint(
        name = "uk_member_provider", columnNames = {"provider", "provider_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    private static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";
    private static final String WITHDRAWN_PROVIDER_ID_PREFIX = "withdrawn:";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(nullable = false)
    private String providerId;

    @Column(nullable = false)
    private String nickname;

    private String profileImageUrl;

    private String profileImageKey;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private LocalDateTime withdrawnAt;

    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean locationSharingEnabled = true;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @ColumnDefault("'PARTICIPANTS'")
    @Column(nullable = false, length = 20)
    private LocationSharingScope locationSharingScope = LocationSharingScope.PARTICIPANTS;

    @ColumnDefault("30")
    @Column(name = "location_sharing_start_minutes", nullable = false)
    private int locationSharingStartMinutes = 30;

    @Builder
    private Member(AuthProvider provider, String providerId, String nickname, String profileImageUrl, String email,
                   Role role) {
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.email = email;
        this.role = role;
    }

    public void withdraw(LocalDateTime withdrawnAt) {
        // 같은 소셜 계정으로 다시 가입할 수 있도록 제공자 회원 ID를 비운다.
        this.providerId = WITHDRAWN_PROVIDER_ID_PREFIX + id;
        this.nickname = WITHDRAWN_NICKNAME;
        removeProfileImage();
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

    public void updateLocationSharingSetting(boolean enabled, LocationSharingScope scope, int startMinutes) {
        this.locationSharingEnabled = enabled;
        this.locationSharingScope = scope;
        this.locationSharingStartMinutes = startMinutes;
    }

    public void removeProfileImage() {
        this.profileImageKey = null;
        this.profileImageUrl = null;
    }

    public boolean isKakaoMember() {
        return provider == AuthProvider.KAKAO;
    }

    public boolean isWithdrawn() {
        return withdrawnAt != null;
    }
}
