package com.jigmnnaga.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;

import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.service.RefreshTokenSaveService;
import com.jigmnnaga.global.jwt.TokenHasher;
import com.jigmnnaga.support.IntegrationTestSupport;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;

class AuthIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RefreshTokenSaveService refreshTokenSaveService;

    @Autowired
    private TokenHasher tokenHasher;

    private String issueRefreshToken(Member member) {
        String refreshToken = jwtProvider.createRefreshToken(member.getId());
        transactionTemplate.executeWithoutResult(status -> refreshTokenSaveService.saveOrRotate(
                memberRepository.findById(member.getId()).orElseThrow(), tokenHasher.hash(refreshToken)));
        return refreshToken;
    }

    @Test
    @DisplayName("Refresh Token으로는 API를 호출할 수 없고, Access Token으로는 재발급할 수 없다")
    void tokenTypesAreSeparated() throws Exception {
        Member member = createMember("나");
        String refreshToken = issueRefreshToken(member);

        mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
        perform(post("/api/v1/auth/reissue"), null, Map.of("refreshToken", jwtProvider.createAccessToken(member.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("재발급하면 Refresh Token이 교체되어 이전 토큰은 거절되고 새 토큰은 계속 사용할 수 있다")
    void reissueRotatesRefreshToken() throws Exception {
        Member member = createMember("나");
        String refreshToken = issueRefreshToken(member);

        JsonNode reissued = data(perform(post("/api/v1/auth/reissue"), null, Map.of("refreshToken", refreshToken))
                .andExpect(status().isOk()));
        String newRefreshToken = reissued.get("refreshToken").asString();

        assertThat(newRefreshToken).isNotEqualTo(refreshToken);
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + reissued.get("accessToken").asString()))
                .andExpect(status().isOk());
        perform(post("/api/v1/auth/reissue"), null, Map.of("refreshToken", refreshToken))
                .andExpect(status().isUnauthorized());
        perform(post("/api/v1/auth/reissue"), null, Map.of("refreshToken", newRefreshToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그아웃하면 Refresh Token으로 재발급할 수 없다")
    void logout() throws Exception {
        Member member = createMember("나");
        String refreshToken = issueRefreshToken(member);

        perform(post("/api/v1/auth/logout"), member).andExpect(status().isOk());

        perform(post("/api/v1/auth/reissue"), null, Map.of("refreshToken", refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("탈퇴하면 익명화되고 기존 Access Token이 차단되며, 방장인 2명 방은 삭제·3명 이상 방은 위임된다")
    void withdraw() throws Exception {
        Member host = createMember("태양");
        Member first = createMember("태희");
        Member second = createMember("하은");
        CreatedAppointment twoMembers = createAppointment(host);
        accept(twoMembers, first);
        CreatedAppointment threeMembers = createAppointment(host);
        accept(threeMembers, first);
        accept(threeMembers, second);

        perform(delete("/api/v1/members/me"), host).andExpect(status().isOk());
        verify(kakaoApiClient).unlink(host.getKakaoId());

        perform(get("/api/v1/notifications/unread-count"), host).andExpect(status().isUnauthorized());
        perform(get(appointmentPath(twoMembers)), first).andExpect(status().isForbidden());
        perform(get(appointmentPath(threeMembers)), first)
                .andExpect(jsonPath("$.data.myRole").value("HOST"))
                .andExpect(jsonPath("$.data.joinedCount").value(2));
        Member withdrawn = memberRepository.findById(host.getId()).orElseThrow();
        assertThat(withdrawn.isWithdrawn()).isTrue();
        assertThat(withdrawn.getNickname()).isEqualTo("탈퇴한 사용자");
    }

    @Test
    @DisplayName("토큰 없이 인증이 필요한 API를 호출하면 401을 반환한다")
    void unauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/appointments/me").param("type", "UPCOMING"))
                .andExpect(status().isUnauthorized());
    }
}
