package mogni.system_backend.turno.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.turno.entity.Turno;

public interface TurnoRepository extends JpaRepository<Turno, Long> {
    List<Turno> findByFechaOrderByHoraInicioAsc(LocalDate fecha);
}
