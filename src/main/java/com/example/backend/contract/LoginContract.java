package com.example.backend.contract;

public interface LoginContract {
    String Login(String email, String password);

    void Logout(String token);
}

