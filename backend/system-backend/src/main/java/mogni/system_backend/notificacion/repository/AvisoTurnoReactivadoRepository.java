package mogni.system_backend.notificacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.notificacion.entity.AvisoTurnoReactivado;

public interface AvisoTurnoReactivadoRepository extends JpaRepository<AvisoTurnoReactivado, Long> {
    List<AvisoTurnoReactivado> findByAlumnoIdOrderByFechaDescIdDesc(Long alumnoId);
}
