package mogni.system_backend.turno.repository;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.turno.entity.FranjaHoraria;

public interface FranjaHorariaRepository extends JpaRepository<FranjaHoraria, Long> {
    List<FranjaHoraria> findByDiaSemanaOrderByHoraInicioAsc(DayOfWeek diaSemana);
}
