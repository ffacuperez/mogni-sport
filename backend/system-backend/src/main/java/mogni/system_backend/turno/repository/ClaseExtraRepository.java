package mogni.system_backend.turno.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mogni.system_backend.turno.entity.ClaseExtra;

public interface ClaseExtraRepository extends JpaRepository<ClaseExtra, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from ClaseExtra c
            where c.alumno.id = :alumnoId
              and c.reservaUsoId is null
              and c.vigenteDesde <= :fecha and c.vigenteHasta >= :fecha
            order by c.vigenteHasta, c.id
            """)
    List<ClaseExtra> buscarDisponiblesConBloqueo(@Param("alumnoId") Long alumnoId,
                                                  @Param("fecha") LocalDate fecha);

    Optional<ClaseExtra> findByReservaUsoId(Long reservaUsoId);
}
