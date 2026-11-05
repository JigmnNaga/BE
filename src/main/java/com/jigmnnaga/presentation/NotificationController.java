package com.jigmnnaga.presentation;

import com.jigmnnaga.application.dto.response.NotificationCountResponse;
import com.jigmnnaga.application.dto.response.NotificationPageResponse;
import com.jigmnnaga.application.usecase.NotificationGetUseCase;
import com.jigmnnaga.application.usecase.NotificationReadUseCase;
import com.jigmnnaga.global.auth.LoginMember;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "알림 관련 API (실시간 수신: STOMP /user/queue/notifications)")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationGetUseCase notificationGetUseCase;
    private final NotificationReadUseCase notificationReadUseCase;

    @Operation(summary = "알림 목록 조회", description = "내 알림을 최신순으로 커서 기반 조회합니다. 응답의 nextCursor를 cursor로 넘기면 이전 알림을 조회합니다.")
    @GetMapping
    public CommonResponse<NotificationPageResponse> getNotifications(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @Parameter(description = "이 알림 ID보다 이전 알림을 조회 (첫 조회 시 생략)") @RequestParam(required = false) Long cursor,
            @Parameter(description = "조회 개수 (1~50)", example = "20") @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        NotificationPageResponse response = notificationGetUseCase.getNotifications(memberId, cursor, size);
        return CommonResponse.success(ResponseMessage.NOTIFICATIONS_GET_SUCCESS, response);
    }

    @Operation(summary = "안 읽은 알림 수 조회", description = "알림 배지에 표시할 안 읽은 알림 수를 조회합니다.")
    @GetMapping("/unread-count")
    public CommonResponse<NotificationCountResponse> getUnreadCount(
            @Parameter(hidden = true) @LoginMember Long memberId
    ) {
        NotificationCountResponse response = notificationGetUseCase.getUnreadCount(memberId);
        return CommonResponse.success(ResponseMessage.NOTIFICATION_UNREAD_COUNT_GET_SUCCESS, response);
    }

    @Operation(summary = "알림 읽음 처리", description = "알림 하나를 읽음 처리합니다.")
    @PatchMapping("/{notificationId}/read")
    public CommonResponse<Void> readNotification(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long notificationId
    ) {
        notificationReadUseCase.readNotification(memberId, notificationId);
        return CommonResponse.success(ResponseMessage.NOTIFICATION_READ_SUCCESS);
    }

    @Operation(summary = "알림 전체 읽음 처리", description = "안 읽은 알림을 모두 읽음 처리하고, 읽음 처리한 개수를 반환합니다.")
    @PatchMapping("/read-all")
    public CommonResponse<NotificationCountResponse> readAllNotifications(
            @Parameter(hidden = true) @LoginMember Long memberId
    ) {
        NotificationCountResponse response = notificationReadUseCase.readAllNotifications(memberId);
        return CommonResponse.success(ResponseMessage.NOTIFICATION_READ_SUCCESS, response);
    }
}
