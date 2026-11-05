package com.jigmnnaga.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InviteIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("초대 정보 조회 시 방장, 응답 인원, 참여 인원, 내 응답 상태를 반환한다")
    void getInviteInfo() throws Exception {
        Member host = createMember("태양");
        Member accepted = createMember("하은");
        Member declined = createMember("채원");
        Member viewer = createMember("나");
        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, accepted);
        perform(post("/api/v1/invites/" + appointment.inviteCode() + "/decline"), declined);

        perform(get("/api/v1/invites/" + appointment.inviteCode()), viewer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hostNickname").value("태양"))
                .andExpect(jsonPath("$.data.respondedCount").value(2))
                .andExpect(jsonPath("$.data.joinedCount").value(2))
                .andExpect(jsonPath("$.data.myStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.participants[0].role").value("HOST"));
    }

    @Test
    @DisplayName("초대를 수락하면 약속 상세를 조회할 수 있고, 중복 응답과 방장 응답은 거절된다")
    void acceptInvite() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        CreatedAppointment appointment = createAppointment(host);
        String path = "/api/v1/invites/" + appointment.inviteCode();

        perform(post(path + "/accept"), member)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointmentId").value(appointment.id()));
        perform(get(appointmentPath(appointment)), member).andExpect(status().isOk());

        perform(post(path + "/accept"), member).andExpect(status().isConflict());
        perform(post(path + "/accept"), host).andExpect(status().isBadRequest());
        perform(get("/api/v1/invites/unknown-code"), member).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("내보내진 멤버는 초대 링크로 다시 참여할 수 없다")
    void kickedMemberCannotRejoin() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, member);

        perform(post(appointmentPath(appointment) + "/participants/" + member.getId() + "/kick"), host)
                .andExpect(status().isOk());

        perform(get("/api/v1/invites/" + appointment.inviteCode()), member)
                .andExpect(jsonPath("$.data.myStatus").value("KICKED"));
        perform(post("/api/v1/invites/" + appointment.inviteCode() + "/accept"), member)
                .andExpect(status().isForbidden());
    }
}
