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

    /** MOGNI-281: si le queda una clase extra para esa fecha. La misma consulta que la anterior, sin bloquear: para leer. */
    @Query("""
            select count(c) > 0 from ClaseExtra c
            where c.alumno.id = :alumnoId
              and c.reservaUsoId is null
              and c.vigenteDesde <= :fecha and c.vigenteHasta >= :fecha
            """)
    boolean existeDisponible(@Param("alumnoId") Long alumnoId, @Param("fecha") LocalDate fecha);

    /**
     * Las clases por recuperar que puede usar en esa fecha, contando solo las de turnos cancelados de la semana que
     * empieza en lunes (o solo las de semanas anteriores). Cada clase vale desde el lunes de la semana del turno
     * cancelado, así que esa fecha dice de qué semana es.
     */
    @Query("""
            select count(c) from ClaseExtra c
            where c.alumno.id = :alumnoId
              and c.reservaUsoId is null
              and c.vigenteDesde <= :fecha and c.vigenteHasta >= :fecha
              and c.vigenteDesde = :lunes
            """)
    long contarDeLaSemana(@Param("alumnoId") Long alumnoId, @Param("fecha") LocalDate fecha,
                          @Param("lunes") LocalDate lunes);

    /** Las que quedaron de semanas anteriores: no suman al contador, pero le dejan reservar con la semana completa. */
    @Query("""
            select count(c) from ClaseExtra c
            where c.alumno.id = :alumnoId
              and c.reservaUsoId is null
              and c.vigenteDesde <= :fecha and c.vigenteHasta >= :fecha
              and c.vigenteDesde < :lunes
            """)
    long contarDeSemanasAnteriores(@Param("alumnoId") Long alumnoId, @Param("fecha") LocalDate fecha,
                                   @Param("lunes") LocalDate lunes);

    Optional<ClaseExtra> findByReservaUsoId(Long reservaUsoId);
}
