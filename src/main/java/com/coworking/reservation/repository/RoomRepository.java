package com.coworking.reservation.repository;

import com.coworking.reservation.model.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>{

    List<Room> findByActiveTrue();

    List<Room> findByCapacityGreaterThanEqual(Integer capacity);

    boolean existsByName(String name);

}
