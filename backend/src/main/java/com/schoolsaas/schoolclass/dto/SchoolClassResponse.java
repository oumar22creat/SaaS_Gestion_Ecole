package com.schoolsaas.schoolclass.dto;

import com.schoolsaas.schoolclass.SchoolClass;

public record SchoolClassResponse(Long id, String name, Long headTeacherId) {

    public static SchoolClassResponse from(SchoolClass schoolClass) {
        return new SchoolClassResponse(schoolClass.getId(), schoolClass.getName(), schoolClass.getHeadTeacherId());
    }
}
