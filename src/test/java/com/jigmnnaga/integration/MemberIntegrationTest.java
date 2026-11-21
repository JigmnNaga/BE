package com.jigmnnaga.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.repository.MemberLocationRepository;
import com.jigmnnaga.global.client.s3.S3FileClient.PresignedUpload;
import com.jigmnnaga.support.IntegrationTestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MemberIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberLocationRepository memberLocationRepository;

    private String uploadImage(Member member) throws Exception {
        when(s3FileClient.presignUpload(anyString(), eq("image/png"), anyLong(), any(Duration.class)))
                .thenReturn(new PresignedUpload("https://s3.example.com/upload", Instant.now().plusSeconds(600)));
        String key = data(perform(post("/api/v1/members/me/profile-image/upload-url"), member,
                Map.of("contentType", "image/png", "contentLength", 1024))
                .andExpect(status().isOk()))
                .get("profileImageKey").asString();
        when(s3FileClient.exists(key)).thenReturn(true);
        when(s3FileClient.presignDownloadUrl(eq(key), any(Duration.class))).thenReturn("https://s3.example.com/" + key);
        return key;
    }

    private Map<String, Object> location(String name, String address, double latitude, double longitude) {
        return Map.of("name", name, "address", address, "latitude", latitude, "longitude", longitude);
    }

    @Test
    @DisplayName("내 정보 조회 시 닉네임, 프로필 이미지, 기본 위치 목록을 반환한다")
    void getMyInfo() throws Exception {
        Member member = createMember("태양");

        perform(get("/api/v1/members/me"), member)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(member.getId()))
                .andExpect(jsonPath("$.data.nickname").value("태양"))
                .andExpect(jsonPath("$.data.profileImageUrl").doesNotExist())
                .andExpect(jsonPath("$.data.locations.length()").value(0));

        perform(post("/api/v1/members/me/locations"), member, location("집", "서울 강남구 테헤란로 152", 37.4979420, 127.0276210))
                .andExpect(status().isOk());

        perform(get("/api/v1/members/me"), member)
                .andExpect(jsonPath("$.data.locations.length()").value(1))
                .andExpect(jsonPath("$.data.locations[0].name").value("집"))
                .andExpect(jsonPath("$.data.locations[0].latitude").value(37.4979420));
    }

    @Test
    @DisplayName("기본 위치를 여러 개 등록·수정·삭제하고 목록으로 조회할 수 있다")
    void manageLocations() throws Exception {
        Member member = createMember("태양");
        String path = "/api/v1/members/me/locations";

        long homeId = data(perform(post(path), member, location("집", "서울 강남구 테헤란로 152", 37.4979420, 127.0276210))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("집")))
                .get("locationId").asLong();
        long officeId = data(perform(post(path), member, location("  회사  ", "서울 중구 세종대로 110", 37.5662952, 126.9779451))
                .andExpect(jsonPath("$.data.name").value("회사")))
                .get("locationId").asLong();

        perform(get(path), member)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].locationId").value(homeId))
                .andExpect(jsonPath("$.data[1].locationId").value(officeId))
                .andExpect(jsonPath("$.data[1].address").value("서울 중구 세종대로 110"));

        perform(put(path + "/" + officeId), member, location("학교", "서울 관악구 관악로 1", 37.4599, 126.9519))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationId").value(officeId))
                .andExpect(jsonPath("$.data.name").value("학교"))
                .andExpect(jsonPath("$.data.longitude").value(126.9519));

        perform(delete(path + "/" + homeId), member).andExpect(status().isOk());
        perform(get(path), member)
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("학교"));
    }

    @Test
    @DisplayName("다른 회원의 위치는 수정·삭제할 수 없고, 기본 위치는 최대 10개까지 저장할 수 있다")
    void locationOwnershipAndLimit() throws Exception {
        Member member = createMember("태양");
        Member other = createMember("태희");
        String path = "/api/v1/members/me/locations";
        long otherLocationId = data(perform(post(path), other, location("집", "서울", 37.5, 127.0)))
                .get("locationId").asLong();

        perform(put(path + "/" + otherLocationId), member, location("내 집", "서울", 37.5, 127.0))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_LOCATION_NOT_FOUND"));
        perform(delete(path + "/" + otherLocationId), member)
                .andExpect(status().isNotFound());
        perform(get(path), other).andExpect(jsonPath("$.data[0].name").value("집"));

        for (int i = 0; i < 10; i++) {
            perform(post(path), member, location("위치" + i, "서울", 37.5, 127.0)).andExpect(status().isOk());
        }
        perform(post(path), member, location("11번째", "서울", 37.5, 127.0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEMBER_LOCATION_LIMIT_EXCEEDED"));
        perform(post(path), member, Map.of("name", "좌표 없음"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @DisplayName("업로드한 이미지 키로 프로필을 수정하면 다른 API에도 임시 조회 URL로 내려가고, 교체 시 기존 이미지를 삭제한다")
    void updateProfileImage() throws Exception {
        Member member = createMember("태양");
        String firstKey = uploadImage(member);
        assertThat(firstKey).startsWith("profiles/" + member.getId() + "/").endsWith(".png");

        perform(patch("/api/v1/members/me/profile"), member, Map.of("nickname", "  햇살  ", "profileImageKey", firstKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("햇살"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://s3.example.com/" + firstKey));

        CreatedAppointment appointment = createAppointment(member);
        perform(get(appointmentPath(appointment)), member)
                .andExpect(status().isOk());
        perform(get("/api/v1/invites/" + appointment.inviteCode()), member)
                .andExpect(jsonPath("$.data.participants[0].profileImageUrl").value("https://s3.example.com/" + firstKey));

        String secondKey = uploadImage(member);
        perform(patch("/api/v1/members/me/profile"), member, Map.of("profileImageKey", secondKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("햇살"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://s3.example.com/" + secondKey));
        verify(s3FileClient, timeout(3000)).delete(firstKey);

        perform(delete("/api/v1/members/me/profile-image"), member).andExpect(status().isOk());
        verify(s3FileClient, timeout(3000)).delete(secondKey);
        perform(get("/api/v1/members/me"), member)
                .andExpect(jsonPath("$.data.profileImageUrl").doesNotExist());
    }

    @Test
    @DisplayName("다른 회원의 키, 업로드되지 않은 키, 허용되지 않은 형식, 빈 수정 요청은 거부한다")
    void rejectInvalidProfileUpdate() throws Exception {
        Member member = createMember("태양");
        Member other = createMember("태희");
        String otherKey = uploadImage(other);

        perform(patch("/api/v1/members/me/profile"), member, Map.of("profileImageKey", otherKey))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PROFILE_IMAGE_KEY"));

        String notUploaded = "profiles/" + member.getId() + "/not-uploaded.png";
        when(s3FileClient.exists(notUploaded)).thenReturn(false);
        perform(patch("/api/v1/members/me/profile"), member, Map.of("profileImageKey", notUploaded))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROFILE_IMAGE_NOT_UPLOADED"));

        perform(post("/api/v1/members/me/profile-image/upload-url"), member,
                Map.of("contentType", "image/gif", "contentLength", 1024))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PROFILE_IMAGE_TYPE"));
        perform(post("/api/v1/members/me/profile-image/upload-url"), member,
                Map.of("contentType", "image/png", "contentLength", 6 * 1024 * 1024))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));

        perform(patch("/api/v1/members/me/profile"), member, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_PROFILE_UPDATE"));
        perform(patch("/api/v1/members/me/profile"), member, Map.of("nickname", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        perform(patch("/api/v1/members/me/profile"), member, Map.of("nickname", "가".repeat(21)))
                .andExpect(status().isBadRequest());

        verify(s3FileClient, never()).delete(startsWith("profiles/" + member.getId()));
    }

    @Test
    @DisplayName("탈퇴하면 직접 올린 프로필 이미지와 기본 위치를 삭제한다")
    void withdrawDeletesProfileImage() throws Exception {
        Member member = createMember("태양");
        String key = uploadImage(member);
        perform(patch("/api/v1/members/me/profile"), member, Map.of("profileImageKey", key)).andExpect(status().isOk());
        perform(post("/api/v1/members/me/locations"), member, Map.of(
                "name", "집", "latitude", 37.5, "longitude", 127.0)).andExpect(status().isOk());

        perform(delete("/api/v1/members/me"), member).andExpect(status().isOk());

        verify(s3FileClient, timeout(3000)).delete(key);
        Member withdrawn = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(withdrawn.getProfileImageKey()).isNull();
        assertThat(memberLocationRepository.findAllByMemberIdOrderByIdAsc(member.getId())).isEmpty();
    }

    @Test
    @DisplayName("위치 공유 설정은 저장 전 기본값으로 조회되고, 저장한 값으로 바뀐다")
    void locationSharingSetting() throws Exception {
        Member member = createMember("태양");
        String path = "/api/v1/members/me/location-sharing";

        perform(get(path), member)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.scope").value("PARTICIPANTS"))
                .andExpect(jsonPath("$.data.startMinutesBefore").value(30));

        perform(put(path), member, Map.of("enabled", false, "scope", "NONE", "startMinutesBefore", 60))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.scope").value("NONE"))
                .andExpect(jsonPath("$.data.startMinutesBefore").value(60));

        perform(get(path), member)
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.scope").value("NONE"))
                .andExpect(jsonPath("$.data.startMinutesBefore").value(60));
    }

    @Test
    @DisplayName("허용되지 않은 시작 시간, 범위, 누락된 항목은 거부한다")
    void rejectInvalidLocationSharingSetting() throws Exception {
        Member member = createMember("태양");
        String path = "/api/v1/members/me/location-sharing";

        perform(put(path), member, Map.of("enabled", true, "scope", "PARTICIPANTS", "startMinutesBefore", 15))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_LOCATION_SHARING_START_MINUTES"));
        perform(put(path), member, Map.of("enabled", true, "scope", "EVERYONE", "startMinutesBefore", 30))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        perform(put(path), member, Map.of("enabled", true, "scope", "PARTICIPANTS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));

        perform(get(path), member)
                .andExpect(jsonPath("$.data.startMinutesBefore").value(30));
    }
}
