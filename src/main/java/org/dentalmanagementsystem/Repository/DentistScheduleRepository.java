package org.dentalmanagementsystem.Repository;

import org.dentalmanagementsystem.Entity.Dentist;
import org.dentalmanagementsystem.Entity.DentistSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface DentistScheduleRepository extends JpaRepository<DentistSchedule, Long> {
    List<DentistSchedule> findByDentist(Dentist dentist);
    DentistSchedule findByDentistAndDayOfWeek(Dentist dentist, DayOfWeek dayOfWeek);
}