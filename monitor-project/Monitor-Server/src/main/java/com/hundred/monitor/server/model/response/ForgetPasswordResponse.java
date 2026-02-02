package com.hundred.monitor.server.model.response;

import com.hundred.monitor.server.model.entity.User;
import lombok.Data;

@Data
public class ForgetPasswordResponse {
    private Boolean success;
    private String message;
    private String token;
    private User user;

    public static ForgetPasswordResponse success(User user, String token) {
        ForgetPasswordResponse response = new ForgetPasswordResponse();
        response.setSuccess(true);
        response.setToken(token);
        response.setUser(user);
        user.setPassword("******");
        return response;
    }

    public static ForgetPasswordResponse error(String message) {
        ForgetPasswordResponse response = new ForgetPasswordResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
