package com.schoolsaas.parent.dto;

import com.schoolsaas.parent.Parent;

public record ParentResponse(Long id, String firstName, String lastName, String email, String phone) {

    public static ParentResponse from(Parent parent) {
        return new ParentResponse(parent.getId(), parent.getFirstName(), parent.getLastName(), parent.getEmail(), parent.getPhone());
    }
}
