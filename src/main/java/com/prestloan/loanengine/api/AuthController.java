package com.prestloan.loanengine.api;

import com.prestloan.loanengine.api.dto.AuthRequest;
import com.prestloan.loanengine.api.dto.AuthResponse;
import com.prestloan.loanengine.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "JWT authentication endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long expirationMinutes;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.expirationMinutes = expirationMinutes;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Authenticate and issue JWT")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        String token = jwtService.generateToken(authentication.getName());
        return new AuthResponse(token, "Bearer", expirationMinutes * 60);
    }
}
