package com.schoolsaas.schoolclass.dto;

import com.schoolsaas.schoolclass.ClassSubjectAssignment;

public record ClassSubjectAssignmentResponse(Long id, Long classId, Long subjectId, Long teacherId) {

    public static ClassSubjectAssignmentResponse from(ClassSubjectAssignment assignment) {
        return new ClassSubjectAssignmentResponse(
                assignment.getId(), assignment.getClassId(), assignment.getSubjectId(), assignment.getTeacherId());
    }
}
