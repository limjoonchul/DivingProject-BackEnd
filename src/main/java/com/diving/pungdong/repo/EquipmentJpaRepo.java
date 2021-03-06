package com.diving.pungdong.repo;

import com.diving.pungdong.domain.equipment.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EquipmentJpaRepo extends JpaRepository<Equipment, Long> {
    void deleteByName(String name);

    Optional<Equipment> findByName(String name);
}
