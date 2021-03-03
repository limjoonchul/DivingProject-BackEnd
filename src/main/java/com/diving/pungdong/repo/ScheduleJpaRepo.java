package com.diving.pungdong.repo;

import com.diving.pungdong.domain.schedule.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleJpaRepo extends JpaRepository<Schedule, Long> {

    @Query("select s from Schedule s join fetch s.scheduleDetails where s.lecture.id = :lectureId")
    List<Schedule> findByLectureId(@Param("lectureId") Long lectureId);
}
