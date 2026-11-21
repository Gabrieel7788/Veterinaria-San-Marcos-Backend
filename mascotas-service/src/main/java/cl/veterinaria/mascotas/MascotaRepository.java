package cl.veterinaria.mascotas;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MascotaRepository
        extends JpaRepository<Mascota, Long> {

    List<Mascota> findByDuenioUsuarioIdOrderByIdDesc(
        Long duenioUsuarioId
    );
}
