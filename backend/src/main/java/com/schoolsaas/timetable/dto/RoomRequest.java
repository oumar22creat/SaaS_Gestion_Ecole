package com.schoolsaas.timetable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomRequest(@NotBlank @Size(max = 100) String name, Integer capacity) {
}
