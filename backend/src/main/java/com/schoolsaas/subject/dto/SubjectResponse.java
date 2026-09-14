package com.schoolsaas.subject.dto;

import com.schoolsaas.subject.Subject;

public record SubjectResponse(Long id, String name, String code, int coefficient) {

    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(subject.getId(), subject.getName(), subject.getCode(), subject.getCoefficient());
    }
}
