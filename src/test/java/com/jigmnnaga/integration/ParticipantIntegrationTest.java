package com.jigmnnaga.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.support.IntegrationTestSupport;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParticipantIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("참여자는 나간 뒤 초대 링크로 다시 참여할 수 있다")
    void leaveAndRejoin() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, member);

        perform(post(appointmentPath(appointment) + "/leave"), member).andExpect(status().isOk());
        perform(get(appointmentPath(appointment)), member).andExpect(status().isForbidden());

        accept(appointment, member);
        perform(get(appointmentPath(appointment)), member).andExpect(status().isOk());
    }

    @Test
    @DisplayName("방장은 새 방장을 지정해야 나갈 수 있고, 혼자 남았으면 나갈 수 없다")
    void hostLeave() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        CreatedAppointment appointment = createAppointment(host);
        String path = appointmentPath(appointment) + "/leave";

        perform(post(path), host, Map.of("newHostMemberId", member.getId())).andExpect(status().isBadRequest());
        accept(appointment, member);
        perform(post(path), host).andExpect(status().isBadRequest());

        perform(post(path), host, Map.of("newHostMemberId", member.getId())).andExpect(status().isOk());
        perform(get(appointmentPath(appointment)), member).andExpect(jsonPath("$.data.myRole").value("HOST"));
    }

    @Test
    @DisplayName("방장은 다른 멤버에게 방장을 위임할 수 있고, 위임 후에는 방장 기능을 쓸 수 없다")
    void delegateHost() throws Exception {
        Member host = createMember("태양");
        Member member = createMember("태희");
        CreatedAppointment appointment = createAppointment(host);
        accept(appointment, member);

        perform(patch(appointmentPath(appointment) + "/host"), member, Map.of("memberId", host.getId()))
                .andExpect(status().isForbidden());
        perform(patch(appointmentPath(appointment) + "/host"), host, Map.of("memberId", member.getId()))
                .andExpect(status().isOk());

        perform(post(appointmentPath(appointment) + "/participants/" + member.getId() + "/kick"), host)
                .andExpect(status().isForbidden());
        perform(get(appointmentPath(appointment)), member).andExpect(jsonPath("$.data.myRole").value("HOST"));
    }

    @Test
    @DisplayName("방장은 자기 자신이나 참여하지 않은 멤버를 내보낼 수 없다")
    void kickValidation() throws Exception {
        Member host = createMember("태양");
        Member outsider = createMember("외부인");
        CreatedAppointment appointment = createAppointment(host);

        perform(post(appointmentPath(appointment) + "/participants/" + host.getId() + "/kick"), host)
                .andExpect(status().isBadRequest());
        perform(post(appointmentPath(appointment) + "/participants/" + outsider.getId() + "/kick"), host)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("약속방 알림 설정을 끄면 상세 조회에 반영된다")
    void notificationSetting() throws Exception {
        Member host = createMember("태양");
        Member outsider = createMember("외부인");
        CreatedAppointment appointment = createAppointment(host);
        String path = appointmentPath(appointment) + "/notification-setting";

        perform(get(appointmentPath(appointment)), host).andExpect(jsonPath("$.data.myNotificationEnabled").value(true));
        perform(patch(path), host, Map.of("enabled", false)).andExpect(status().isOk());
        perform(get(appointmentPath(appointment)), host).andExpect(jsonPath("$.data.myNotificationEnabled").value(false));
        perform(patch(path), outsider, Map.of("enabled", false)).andExpect(status().isForbidden());
    }
}
