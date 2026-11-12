package com.jigmnnaga.global.exception;

import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import com.jigmnnaga.global.response.CommonResponse;
import com.jigmnnaga.global.response.ResponseMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailureController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void unexpectedFailureIsLoggedButNotExposed(CapturedOutput output) throws Exception {
        String body = mockMvc.perform(get("/test/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()))
                .andExpect(jsonPath("$.data").isEmpty())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("private-database-detail", "IllegalStateException");
        assertThat(output.getAll()).contains("Unexpected server error",
                "java.lang.IllegalStateException: private-database-detail", "FailureController.fail");
    }

    @Test
    void unsupportedMethodKeepsStatusAndAllowHeader() throws Exception {
        mockMvc.perform(patch("/test/failure"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("REQUEST_NOT_SUPPORTED"));
    }

    @Test
    void businessExceptionReturnsErrorCodeName() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVITE_EXPIRED"))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVITE_EXPIRED.getMessage()));
    }

    @Test
    void invalidRequestParameterReturnsInvalidInputCode() throws Exception {
        mockMvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void successResponseOmitsCode() throws Exception {
        mockMvc.perform(get("/test/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @RestController
    static class FailureController {
        @GetMapping("/test/failure")
        String fail() {
            throw new IllegalStateException("private-database-detail");
        }

        @GetMapping("/test/business")
        String business() {
            throw new BusinessException(ErrorCode.INVITE_EXPIRED);
        }

        @GetMapping("/test/param")
        String param(@RequestParam Long value) {
            return "ok";
        }

        @GetMapping("/test/success")
        CommonResponse<Void> success() {
            return CommonResponse.success(ResponseMessage.INVITE_DECLINE_SUCCESS);
        }
    }
}
