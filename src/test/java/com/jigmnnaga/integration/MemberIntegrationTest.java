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
import com.jigmnnaga.global.client.s3.S3FileClient.PresignedUpload;
import com.jigmnnaga.support.IntegrationTestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberIntegrationTest extends IntegrationTestSupport {

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

    @Test
    @DisplayName("내 정보 조회 시 닉네임, 프로필 이미지, 기본 위치를 반환한다")
    void getMyInfo() throws Exception {
        Member member = createMember("태양");

        perform(get("/api/v1/members/me"), member)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(member.getId()))
                .andExpect(jsonPath("$.data.nickname").value("태양"))
                .andExpect(jsonPath("$.data.profileImageUrl").doesNotExist())
                .andExpect(jsonPath("$.data.defaultLocation").doesNotExist());

        perform(put("/api/v1/members/me/default-location"), member, Map.of(
                "name", "우리집", "address", "서울 강남구 테헤란로 152", "latitude", 37.4979420, "longitude", 127.0276210))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultLocation.name").value("우리집"));

        perform(get("/api/v1/members/me"), member)
                .andExpect(jsonPath("$.data.defaultLocation.address").value("서울 강남구 테헤란로 152"))
                .andExpect(jsonPath("$.data.defaultLocation.latitude").value(37.4979420))
                .andExpect(jsonPath("$.data.defaultLocation.longitude").value(127.0276210));

        perform(delete("/api/v1/members/me/default-location"), member).andExpect(status().isOk());
        perform(get("/api/v1/members/me"), member)
                .andExpect(jsonPath("$.data.defaultLocation").doesNotExist());
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
        perform(put("/api/v1/members/me/default-location"), member, Map.of(
                "name", "우리집", "latitude", 37.5, "longitude", 127.0)).andExpect(status().isOk());

        perform(delete("/api/v1/members/me"), member).andExpect(status().isOk());

        verify(s3FileClient, timeout(3000)).delete(key);
        Member withdrawn = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(withdrawn.getProfileImageKey()).isNull();
        assertThat(withdrawn.hasDefaultLocation()).isFalse();
    }
}
