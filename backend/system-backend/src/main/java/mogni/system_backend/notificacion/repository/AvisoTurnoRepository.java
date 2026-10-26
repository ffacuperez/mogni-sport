package mogni.system_backend.notificacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.notificacion.entity.AvisoTurno;

public interface AvisoTurnoRepository extends JpaRepository<AvisoTurno, Long> {
    List<AvisoTurno> findByAlumnoIdOrderByFechaDescIdDesc(Long alumnoId);
}
