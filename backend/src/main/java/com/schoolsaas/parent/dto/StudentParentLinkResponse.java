package com.schoolsaas.parent.dto;

import com.schoolsaas.parent.StudentParent;

public record StudentParentLinkResponse(Long studentId, Long parentId, String relationship, boolean primaryContact) {

    public static StudentParentLinkResponse from(StudentParent link) {
        return new StudentParentLinkResponse(link.getStudentId(), link.getParentId(), link.getRelationship(), link.isPrimaryContact());
    }
}
