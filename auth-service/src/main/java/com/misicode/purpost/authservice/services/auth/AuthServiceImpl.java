package com.misicode.purpost.authservice.services.auth;

import com.misicode.purpost.authservice.client.UserClient;
import com.misicode.purpost.authservice.dto.UserResponse;
import com.misicode.purpost.authservice.payload.SigninRequest;
import com.misicode.purpost.authservice.payload.SigninResponse;
import com.misicode.purpost.authservice.security.jwt.JwtUtils;
import com.misicode.purpost.authservice.services.userdetails.UserDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements IAuthService {
    private AuthenticationManager authenticationManager;
    private JwtUtils jwtUtils;
    private UserClient userClient;

    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtUtils jwtUtils, UserClient userClient) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userClient = userClient;
    }

    @Override
    public SigninResponse login(SigninRequest signinRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            signinRequest.getEmail(),
                            signinRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            String token = jwtUtils.generateJwtToken(userDetails.getUsername());

            UserResponse user = userClient.getUserByEmail(userDetails.getUsername());

            return new SigninResponse(token, user);
        } catch(AuthenticationException e) {
            System.out.println("ERROR: " + e.getMessage());
            return null;
        }
    }

    @Override
    public SigninResponse checkToken(String token) {
        if(token.startsWith("Bearer ")){
            String splitToken = token.substring(7);

            if(jwtUtils.isValidJwtToken(splitToken)){
                String username = jwtUtils.getUsernameFromToken(splitToken);

                UserResponse user = userClient.getUserByEmail(username);

                return new SigninResponse(splitToken, user);
            }
        }
    }

    @Override
    public UserDetailsImpl getUserAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return (UserDetailsImpl) authentication.getPrincipal();
    }

    @Override
    public String getUsernameAuthenticated() {
        return getUserAuthenticated().getUsername();
    }
}
