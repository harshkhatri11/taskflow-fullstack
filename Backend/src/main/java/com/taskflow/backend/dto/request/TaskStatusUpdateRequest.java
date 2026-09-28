package com.taskflow.backend.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


public record TaskStatusUpdateRequest(

        @NotBlank
        @Pattern(regexp = "TODO|IN_PROGRESS|DONE", message = "Status should be TODO, IN_PROGRESS or DONE")
        String status
) {
}
