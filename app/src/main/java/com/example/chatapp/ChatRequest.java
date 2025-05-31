package com.example.chatapp;

public class ChatRequest {
    private String request_type;

    public ChatRequest() {
        // Constructor vacío necesario para Firebase
    }

    public String getRequest_type() {
        return request_type;
    }

    public void setRequest_type(String request_type) {
        this.request_type = request_type;
    }
}
