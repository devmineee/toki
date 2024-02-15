package com.toki.backend.room.repository;

import com.toki.backend.room.entity.Category;
import com.toki.backend.room.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, String> {
    Optional<Room> findByParentRoomPk(String parentRoomPk);
    Page<Room> findAllByCategory(PageRequest pageRequest, Category category);

}
