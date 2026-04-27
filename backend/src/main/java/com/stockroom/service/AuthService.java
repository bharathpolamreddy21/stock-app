package com.stockroom.service;

import com.stockroom.config.AppProperties;
import com.stockroom.config.JwtUtil;
import com.stockroom.dto.LoginRequest;
import com.stockroom.dto.LoginResponse;
import com.stockroom.model.User;
import com.stockroom.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtUtil               jwtUtil;
    private final UserRepository        userRepository;
    private final AppProperties         props;

    public AuthService(AuthenticationManager authManager,
                       JwtUtil jwtUtil,
                       UserRepository userRepository,
                       AppProperties props) {
        this.authManager    = authManager;
        this.jwtUtil        = jwtUtil;
        this.userRepository = userRepository;
        this.props          = props;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid username or password");
        }

        User   user  = userRepository.findByUsername(request.username()).orElseThrow();
        String token = jwtUtil.generateToken(user.getUsername());

        return new LoginResponse(
                token,
                user.getUsername(),
                user.getRole(),
                props.getJwt().getExpiryHours() * 3600L
        );
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow();
    }
}
