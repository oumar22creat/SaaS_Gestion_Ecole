package com.schoolsaas.timetable;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.timetable.dto.RoomRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional
    public Room create(RoomRequest request) {
        return roomRepository.save(new Room(request.name(), request.capacity()));
    }

    public Room getById(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> ApiException.notFound("ROOM_NOT_FOUND", "Salle introuvable"));
    }

    public Page<Room> list(Pageable pageable) {
        return roomRepository.findAll(pageable);
    }

    @Transactional
    public Room update(Long id, RoomRequest request) {
        Room room = getById(id);
        room.setName(request.name());
        room.setCapacity(request.capacity());
        return room;
    }

    @Transactional
    public void delete(Long id) {
        roomRepository.delete(getById(id));
    }
}
