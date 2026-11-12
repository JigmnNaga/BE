package com.jigmnnaga.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.Role;
import com.jigmnnaga.domain.repository.MemberRepository;
import com.jigmnnaga.global.client.fcm.FcmClient;
import com.jigmnnaga.global.client.kakao.KakaoApiClient;
import com.jigmnnaga.global.client.s3.S3FileClient;
import com.jigmnnaga.global.jwt.JwtProvider;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    private static final AtomicLong KAKAO_ID_SEQUENCE = new AtomicLong(System.currentTimeMillis() * 1000);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MemberRepository memberRepository;

    @Autowired
    protected JwtProvider jwtProvider;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @MockitoBean
    protected FcmClient fcmClient;

    @MockitoBean
    protected KakaoApiClient kakaoApiClient;

    @MockitoBean
    protected S3FileClient s3FileClient;

    protected final JsonMapper jsonMapper = JsonMapper.builder().build();

    protected Member createMember(String nickname) {
        return memberRepository.save(Member.builder()
                .kakaoId(KAKAO_ID_SEQUENCE.incrementAndGet())
                .nickname(nickname)
                .role(Role.USER)
                .build());
    }

    protected String bearer(Member member) {
        return "Bearer " + jwtProvider.createAccessToken(member.getId());
    }

    protected ResultActions perform(MockHttpServletRequestBuilder request, Member member, Object body) throws Exception {
        if (member != null) {
            request.header("Authorization", bearer(member));
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body));
        }
        return mockMvc.perform(request);
    }

    protected ResultActions perform(MockHttpServletRequestBuilder request, Member member) throws Exception {
        return perform(request, member, null);
    }

    protected JsonNode data(ResultActions resultActions) throws Exception {
        String body = resultActions.andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(body).get("data");
    }

    protected LocalDate daysLater(int days) {
        return LocalDate.now().plusDays(days);
    }

    protected Map<String, Object> location(String placeName) {
        Map<String, Object> location = new HashMap<>();
        location.put("placeName", placeName);
        location.put("address", "서울");
        location.put("latitude", 37.5);
        location.put("longitude", 127.0);
        return location;
    }

    protected Map<String, Object> createRequest(String dateSelectionType, List<LocalDate> dates, List<String> places,
                                                LocalDate deadlineDate) {
        Map<String, Object> request = new HashMap<>();
        request.put("title", "중간고사 카공 모임");
        request.put("dateSelectionType", dateSelectionType);
        request.put("candidateDates", dates.stream().map(LocalDate::toString).toList());
        request.put("candidateLocations", places.stream().map(this::location).toList());
        request.put("responseDeadline", deadlineDate + "T23:59:59");
        return request;
    }

    protected CreatedAppointment createAppointment(Member host) throws Exception {
        LocalDate date = daysLater(10);
        return createAppointment(host, createRequest("SINGLE", List.of(date), List.of("강남역", "잠실역"), date.minusDays(1)));
    }

    protected CreatedAppointment createAppointment(Member host, Map<String, Object> request) throws Exception {
        JsonNode created = data(perform(post("/api/v1/appointments"), host, request));
        long appointmentId = created.get("appointmentId").asLong();

        JsonNode detail = data(perform(get("/api/v1/appointments/" + appointmentId), host));
        List<Long> locationIds = new ArrayList<>();
        detail.get("candidateLocations").forEach(location -> locationIds.add(location.get("locationId").asLong()));
        return new CreatedAppointment(appointmentId, created.get("inviteCode").asString(), locationIds);
    }

    protected void accept(CreatedAppointment appointment, Member member) throws Exception {
        perform(post("/api/v1/invites/" + appointment.inviteCode() + "/accept"), member);
    }

    protected String appointmentPath(CreatedAppointment appointment) {
        return "/api/v1/appointments/" + appointment.id();
    }

    protected record CreatedAppointment(long id, String inviteCode, List<Long> locationIds) {
    }
}
