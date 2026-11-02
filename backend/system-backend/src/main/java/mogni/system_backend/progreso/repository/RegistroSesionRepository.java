package mogni.system_backend.progreso.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.progreso.entity.RegistroSesion;

public interface RegistroSesionRepository extends JpaRepository<RegistroSesion, Long> {

    List<RegistroSesion> findByPlanId(Long planId);
}
