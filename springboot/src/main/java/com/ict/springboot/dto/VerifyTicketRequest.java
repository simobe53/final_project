package com.ict.springboot.dto;

public class VerifyTicketRequest {
        private String object_key;

    public VerifyTicketRequest(String object_key) {
        this.object_key = object_key;
    }

    public String getObject_key() {
        return object_key;
    }

    public void setObject_key(String object_key) {
        this.object_key = object_key;
    }
}
