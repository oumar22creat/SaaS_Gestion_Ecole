package com.schoolsaas.timetable.dto;

import com.schoolsaas.timetable.Room;

public record RoomResponse(Long id, String name, Integer capacity) {

    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getName(), room.getCapacity());
    }
}
