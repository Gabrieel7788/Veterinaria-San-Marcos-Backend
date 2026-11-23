package cl.veterinaria.mascotas;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

public interface AtencionClinicaRepository
        extends JpaRepository<AtencionClinica, Long> {

    List<AtencionClinica>
        findByMascotaIdOrderByFechaAtencionDescIdDesc(
            Long mascotaId
        );

    List<AtencionClinica>
        findByFechaAtencionBetweenOrderByFechaAtencionDescIdDesc(
            LocalDate desde,
            LocalDate hasta
        );

}
