package mogni.system_backend.pago.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import mogni.system_backend.pago.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {
}
