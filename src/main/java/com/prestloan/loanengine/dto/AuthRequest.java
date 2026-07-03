package com.prestloan.loanengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 100, message = "username length must be between 3 and 100 characters")
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 6, max = 100, message = "password length must be between 6 and 100 characters")
        String password) {

}
