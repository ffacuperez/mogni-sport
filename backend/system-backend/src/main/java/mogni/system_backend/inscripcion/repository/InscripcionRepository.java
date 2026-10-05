package mogni.system_backend.inscripcion.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import mogni.system_backend.inscripcion.entity.EstadoInscripcion;
import mogni.system_backend.inscripcion.entity.Inscripcion;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    boolean existsByMembresiaIdAndEstado(Long membresiaId, EstadoInscripcion estado);

    boolean existsByMembresiaId(Long membresiaId);

    /** Pasa de ACTIVA a VENCIDA todas las inscripciones cuya fecha de fin es anterior a hoy. */
    @Transactional
    @Modifying
    @Query("update Inscripcion i set i.estado = :vencida where i.estado = :activa and i.fechaFin < :hoy")
    int marcarVencidas(@Param("hoy") LocalDate hoy,
                       @Param("activa") EstadoInscripcion activa,
                       @Param("vencida") EstadoInscripcion vencida);
}
