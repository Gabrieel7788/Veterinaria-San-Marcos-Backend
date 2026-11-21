package cl.veterinaria.citas;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface CitaRepository
        extends JpaRepository<Cita, Long> {

    List<Cita> findByDuenoUsuarioIdOrderByIdDesc(
        Long duenoUsuarioId
    );

    boolean existsByEstadoAndFechaHoraAfterAndFechaHoraBefore(
        String estado,
        LocalDateTime inicio,
        LocalDateTime fin
    );

    boolean existsByEstadoInAndFechaHoraAfterAndFechaHoraBefore(
        List<String> estados,
        LocalDateTime inicio,
        LocalDateTime fin
    );
                                            

}
