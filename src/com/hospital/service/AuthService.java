package com.hospital.service;

import com.hospital.dao.UserDAO;
import com.hospital.model.User;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private static final Map<String, User> SESSIONS = new ConcurrentHashMap<>();
    private static final UserDAO userDAO = new UserDAO();

    public static User authenticate(String username, String password) {
        return userDAO.authenticate(username, password);
    }

    public Map<String, Object> login(String username, String password) {
        Map<String, Object> response = new ConcurrentHashMap<>();
        User user = userDAO.authenticate(username, password);
        if (user != null) {
            String token = UUID.randomUUID().toString();
            SESSIONS.put(token, user);

            response.put("success", true);
            response.put("token", token);
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("role", user.getRole());
            response.put("email", user.getEmail());
            response.put("hospitalId", user.getHospitalId());
        } else {
            response.put("success", false);
            response.put("message", "Invalid username or password");
        }
        return response;
    }

    public static User getUserByToken(String token) {
        if (token == null) return null;
        return SESSIONS.get(token);
    }

    public boolean logout(String token) {
        if (token != null) {
            SESSIONS.remove(token);
            return true;
        }
        return false;
    }
}
