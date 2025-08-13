package com.xinyu.InterviewCoach_v2.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Token验证请求DTO
 */
public class TokenRequestDTO {

    @NotBlank(message = "Token不能为空")
    private String token;

    public TokenRequestDTO() {}

    public TokenRequestDTO(String token) {
        this.token = token;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "TokenRequestDTO{" +
                "token='[PROTECTED]'" +
                '}';
    }
}