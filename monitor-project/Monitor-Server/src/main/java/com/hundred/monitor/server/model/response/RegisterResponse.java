package com.hundred.monitor.server.model.response;

import com.hundred.monitor.server.model.entity.User;
import lombok.Data;

@Data
public class RegisterResponse {
    private Boolean success;
    private String message;
    private String token;
    private User user;

    public static RegisterResponse success(User user, String token) {
        RegisterResponse response = new RegisterResponse();
        response.setSuccess(true);
        response.setToken(token);
        response.setUser(user);
        user.setPassword("******");
        return response;
    }

    public static RegisterResponse error(String message) {
        RegisterResponse response = new RegisterResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
