package com.terramap.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Advertiser contact details")
public record ContactDto(
        @Schema(example = "Jane Doe")
        @NotBlank @Size(max = 120) String name,

        @Schema(example = "jane@example.com")
        @NotBlank @Email @Size(max = 180) String email,

        @Schema(example = "+55 11 90000-0000")
        @Size(max = 30) String phone
) {}
