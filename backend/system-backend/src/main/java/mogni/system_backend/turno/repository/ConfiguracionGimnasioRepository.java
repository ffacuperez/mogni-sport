package mogni.system_backend.turno.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.turno.entity.ConfiguracionGimnasio;

public interface ConfiguracionGimnasioRepository extends JpaRepository<ConfiguracionGimnasio, Long> {
    /** La única fila de configuración (si por algún motivo hubiera más, siempre se usa la primera). */
    Optional<ConfiguracionGimnasio> findFirstByOrderByIdAsc();
}
