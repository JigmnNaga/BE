package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.request.AppointmentCreateRequest;
import com.jigmnnaga.application.dto.request.AppointmentTitleUpdateRequest;
import com.jigmnnaga.application.dto.request.CandidateDatesUpdateRequest;
import com.jigmnnaga.application.dto.request.CandidateLocationsUpdateRequest;
import com.jigmnnaga.application.dto.request.ConfirmDateTimeRequest;
import com.jigmnnaga.application.dto.request.ConfirmLocationRequest;
import com.jigmnnaga.application.dto.request.ResponseDeadlineUpdateRequest;
import com.jigmnnaga.application.dto.response.AppointmentCreateResponse;
import com.jigmnnaga.application.dto.response.AppointmentDetailResponse;
import com.jigmnnaga.application.usecase.AppointmentCancelUseCase;
import com.jigmnnaga.application.usecase.AppointmentConfirmUseCase;
import com.jigmnnaga.application.usecase.AppointmentCreateUseCase;
import com.jigmnnaga.application.usecase.AppointmentDeleteUseCase;
import com.jigmnnaga.application.usecase.AppointmentDetailGetUseCase;
import com.jigmnnaga.application.usecase.AppointmentUpdateUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Appointment", description = "약속방 관련 API")
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentCreateUseCase appointmentCreateUseCase;
    private final AppointmentDetailGetUseCase appointmentDetailGetUseCase;
    private final AppointmentConfirmUseCase appointmentConfirmUseCase;
    private final AppointmentUpdateUseCase appointmentUpdateUseCase;
    private final AppointmentCancelUseCase appointmentCancelUseCase;
    private final AppointmentDeleteUseCase appointmentDeleteUseCase;

    @Operation(summary = "약속방 생성", description = "약속 이름, 후보 날짜, 후보 위치(최대 5개), 응답 기한으로 약속방을 생성하고 초대 링크를 발급합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<AppointmentCreateResponse> createAppointment(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @RequestBody @Valid AppointmentCreateRequest request
    ) {
        AppointmentCreateResponse response = appointmentCreateUseCase.createAppointment(memberId, request);
        return CommonResponse.success(ResponseMessage.APPOINTMENT_CREATE_SUCCESS, response);
    }

    @Operation(summary = "약속 상세 조회", description = "약속 정보, 후보 날짜·장소, 응답 기한, 초대 링크, 내 역할, 참여 멤버와 일정 선택 여부를 조회합니다.")
    @GetMapping("/{appointmentId}")
    public CommonResponse<AppointmentDetailResponse> getAppointmentDetail(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId
    ) {
        AppointmentDetailResponse response = appointmentDetailGetUseCase.getAppointmentDetail(memberId, appointmentId);
        return CommonResponse.success(ResponseMessage.APPOINTMENT_DETAIL_GET_SUCCESS, response);
    }

    @Operation(summary = "약속 날짜·시간 확정", description = "방장이 후보 날짜 중 하나와 시간을 약속 일정으로 확정합니다. 다시 호출하면 변경됩니다.")
    @PutMapping("/{appointmentId}/confirmation/date-time")
    public CommonResponse<Void> confirmDateTime(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId,
            @RequestBody @Valid ConfirmDateTimeRequest request
    ) {
        appointmentConfirmUseCase.confirmDateTime(memberId, appointmentId, request);
        return CommonResponse.success(ResponseMessage.CONFIRM_DATE_TIME_SUCCESS);
    }

    @Operation(summary = "약속 장소 확정", description = "방장이 후보 장소 중 하나를 확정하고 세부 위치(상호)를 함께 정합니다. 세부 위치를 나중에 정하면 detailPlace를 생략합니다. 다시 호출하면 변경됩니다.")
    @PutMapping("/{appointmentId}/confirmation/location")
    public CommonResponse<Void> confirmLocation(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId,
            @RequestBody @Valid ConfirmLocationRequest request
    ) {
        appointmentConfirmUseCase.confirmLocation(memberId, appointmentId, request);
        return CommonResponse.success(ResponseMessage.CONFIRM_LOCATION_SUCCESS);
    }

    @Operation(summary = "약속 이름 변경", description = "방장이 약속 이름을 변경합니다.")
    @PatchMapping("/{appointmentId}/title")
    public CommonResponse<Void> updateTitle(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId,
            @RequestBody @Valid AppointmentTitleUpdateRequest request
    ) {
        appointmentUpdateUseCase.updateTitle(memberId, appointmentId, request);
        return CommonResponse.success(ResponseMessage.APPOINTMENT_UPDATE_SUCCESS);
    }

    @Operation(summary = "후보 날짜 변경", description = "방장이 날짜 선택 방식과 후보 날짜를 변경합니다. 빠진 날짜의 멤버 가능 시간은 삭제되고, 확정 날짜가 빠지면 날짜·시간 확정이 해제됩니다.")
    @PutMapping("/{appointmentId}/candidate-dates")
    public CommonResponse<Void> updateCandidateDates(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId,
            @RequestBody @Valid CandidateDatesUpdateRequest request
    ) {
        appointmentUpdateUseCase.updateCandidateDates(memberId, appointmentId, request);
        return CommonResponse.success(ResponseMessage.APPOINTMENT_UPDATE_SUCCESS);
    }

    @Operation(summary = "후보 장소 변경", description = "방장이 유지할 기존 장소와 새 장소로 후보 장소를 변경합니다(최대 5개). 삭제된 장소를 희망한 멤버의 선택과 확정 장소는 해제됩니다.")
    @PutMapping("/{appointmentId}/candidate-locations")
    public CommonResponse<Void> updateCandidateLocations(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId,
            @RequestBody @Valid CandidateLocationsUpdateRequest request
    ) {
        appointmentUpdateUseCase.updateCandidateLocations(memberId, appointmentId, request);
        return CommonResponse.success(ResponseMessage.APPOINTMENT_UPDATE_SUCCESS);
    }

    @Operation(summary = "응답 기한 변경", description = "방장이 참여 응답 기한을 변경합니다. 현재 이후이면서 첫 후보 날짜 이전이어야 합니다.")
    @PatchMapping("/{appointmentId}/response-deadline")
    public CommonResponse<Void> updateResponseDeadline(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId,
            @RequestBody @Valid ResponseDeadlineUpdateRequest request
    ) {
        appointmentUpdateUseCase.updateResponseDeadline(memberId, appointmentId, request);
        return CommonResponse.success(ResponseMessage.APPOINTMENT_UPDATE_SUCCESS);
    }

    @Operation(summary = "약속 취소", description = "방장이 약속을 취소합니다. 취소된 약속은 수정·초대 응답·일정 선택·확정·참석 응답이 불가합니다.")
    @PostMapping("/{appointmentId}/cancel")
    public CommonResponse<Void> cancelAppointment(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId
    ) {
        appointmentCancelUseCase.cancelAppointment(memberId, appointmentId);
        return CommonResponse.success(ResponseMessage.APPOINTMENT_CANCEL_SUCCESS);
    }

    @Operation(summary = "약속방 삭제", description = "방장이 약속방과 관련 데이터(후보 날짜·장소, 참여자, 일정 선택)를 모두 삭제합니다. 복구할 수 없습니다.")
    @DeleteMapping("/{appointmentId}")
    public CommonResponse<Void> deleteAppointment(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long appointmentId
    ) {
        appointmentDeleteUseCase.deleteAppointment(memberId, appointmentId);
        return CommonResponse.success(ResponseMessage.APPOINTMENT_DELETE_SUCCESS);
    }
}
