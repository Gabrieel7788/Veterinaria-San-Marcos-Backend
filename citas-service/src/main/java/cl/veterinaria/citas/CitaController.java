
package cl.veterinaria.citas;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaRepository repositorio;
    private final RestClient cliente = RestClient.create();

    public CitaController(CitaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public record SolicitudCita(
        @NotNull Long mascotaId,

        @NotBlank
        @Size(max = 100)
        String servicio,

        @NotBlank
        @Size(max = 500)
        String motivo,

        @NotNull
        LocalDateTime fechaHora
    ) {}

    private Long obtenerDueno(Jwt jwt) {
        if (!"DUENO_MASCOTA".equals(
                jwt.getClaimAsString("rol"))) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo los duenos pueden solicitar citas"
            );
        }

        return Long.valueOf(jwt.getSubject());
    }

    private void comprobarMascota(
            Long mascotaId, Jwt jwt) {

        try {
                String urlBase = System.getenv().getOrDefault(
        "MASCOTAS_SERVICE_URL",
        "http://localhost:8081"
    );
            cliente.get()
                .uri(
                    urlBase + "/api/mascotas/{id}",
                    mascotaId
                )
                .headers(h -> h.setBearerAuth(
                    jwt.getTokenValue()
                ))
                .retrieve()
                .toBodilessEntity();

        } catch (HttpClientErrorException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La mascota no existe o no pertenece al usuario"
            );
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo consultar mascotas-service"
            );
        }
    }

    @PostMapping
    public ResponseEntity<Cita> crear(
            @Valid @RequestBody SolicitudCita datos,
            @AuthenticationPrincipal Jwt jwt) {

        Long duenoId = obtenerDueno(jwt);

        comprobarMascota(datos.mascotaId(), jwt);

        LocalDateTime ahora = LocalDateTime.now(
            ZoneId.of("America/Santiago")
        );

        if (!datos.fechaHora().isAfter(ahora)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La fecha de la cita debe ser futura"
            );
        }

        validarHorarioAtencion(datos.fechaHora());
        verificarHorarioLibreParaSolicitud(datos.fechaHora());

        Cita cita = new Cita();

        cita.setDuenoUsuarioId(duenoId);
        cita.setMascotaId(datos.mascotaId());
        cita.setServicio(datos.servicio().trim());
        cita.setMotivo(datos.motivo().trim());
        cita.setFechaHora(datos.fechaHora());
        cita.setEstado("PENDIENTE");
        cita.setCreadaEn(ahora);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(repositorio.save(cita));
    }

    @GetMapping("/mias")
    public List<Cita> misCitas(
            @AuthenticationPrincipal Jwt jwt) {

        Long duenoId = obtenerDueno(jwt);

        return repositorio
            .findByDuenoUsuarioIdOrderByIdDesc(duenoId);
    }

    private void exigirRecepcion(Jwt jwt) {

        String rol = jwt.getClaimAsString("rol");

        if (!"RECEPCIONISTA".equals(rol)
                && !"ADMINISTRADOR".equals(rol)) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tienes permiso para gestionar citas"
            );
        }
    }

    @GetMapping("/recepcion")
    public List<Cita> listarTodasLasCitas(
            @AuthenticationPrincipal Jwt jwt) {

        exigirRecepcion(jwt);

        return repositorio.findAll();
    }


@PatchMapping("/{id}/confirmar")
public Cita confirmarCita(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt) {

    exigirRecepcion(jwt);

    Cita cita = repositorio.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Cita no encontrada"
        ));

    if (!"PENDIENTE".equals(cita.getEstado())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "La cita no esta pendiente"
        );
    }

    LocalDateTime ahora = LocalDateTime.now(
        ZoneId.of("America/Santiago")
    );

    if (!cita.getFechaHora().isAfter(ahora)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "No se puede confirmar una fecha pasada"
        );
    }
    validarHorarioAtencion(cita.getFechaHora());
    verificarHorarioDisponible(cita.getFechaHora());

    cita.setEstado("CONFIRMADA");

    return repositorio.save(cita);
}

public record PropuestaHorario(
    @NotNull LocalDateTime nuevaFechaHora
) {}

@PatchMapping("/{id}/proponer-horario")
public Cita proponerHorario(
        @PathVariable Long id,
        @Valid @RequestBody PropuestaHorario datos,
        @AuthenticationPrincipal Jwt jwt) {

    exigirRecepcion(jwt);

    Cita cita = repositorio.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Cita no encontrada"
        ));

    if (!"PENDIENTE".equals(cita.getEstado())
            && !"PROPUESTA".equals(cita.getEstado())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "No se puede proponer un horario para esta cita"
        );
    }

    LocalDateTime ahora = LocalDateTime.now(
        ZoneId.of("America/Santiago")
    );

    if (!datos.nuevaFechaHora().isAfter(ahora)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "La nueva fecha debe ser futura"
        );
    }

    validarHorarioAtencion(datos.nuevaFechaHora());
    verificarHorarioDisponible(datos.nuevaFechaHora());
    cita.setFechaHoraPropuesta(datos.nuevaFechaHora());
    cita.setEstado("PROPUESTA");

    return repositorio.save(cita);
}


@PatchMapping("/{id}/aceptar-propuesta")
public Cita aceptarPropuesta(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt) {

    Long duenoId = obtenerDueno(jwt);

    Cita cita = repositorio.findById(id)
        .filter(c -> c.getDuenoUsuarioId().equals(duenoId))
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Cita no encontrada"
        ));

    if (!"PROPUESTA".equals(cita.getEstado())
            || cita.getFechaHoraPropuesta() == null) {

        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Esta cita no tiene una propuesta pendiente"
        );
    }

    LocalDateTime ahora = LocalDateTime.now(
        ZoneId.of("America/Santiago")
    );

    if (!cita.getFechaHoraPropuesta().isAfter(ahora)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "El horario propuesto ya paso"
        );
    }

    validarHorarioAtencion(cita.getFechaHoraPropuesta());
    verificarHorarioDisponible(cita.getFechaHoraPropuesta());
    cita.setFechaHora(cita.getFechaHoraPropuesta());
    cita.setFechaHoraPropuesta(null);
    cita.setEstado("CONFIRMADA");

    return repositorio.save(cita);
}

private void verificarHorarioDisponible(
        LocalDateTime fechaHora) {

    boolean ocupado = repositorio
        .existsByEstadoAndFechaHoraAfterAndFechaHoraBefore(
            "CONFIRMADA",
            fechaHora.minusMinutes(30),
            fechaHora.plusMinutes(30)
        );

    if (ocupado) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Ya existe una cita confirmada en ese horario"
        );
    }
}

private void validarHorarioAtencion(
        LocalDateTime fechaHora) {

    // De lunes a viernes
    if (fechaHora.getDayOfWeek().getValue() > 5) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "La veterinaria atiende de lunes a viernes"
        );
    }

    LocalTime hora = fechaHora.toLocalTime();

    LocalTime apertura = LocalTime.of(9, 0);
    LocalTime cierre = LocalTime.of(18, 0);

    if (hora.isBefore(apertura)
            || !hora.isBefore(cierre)) {

        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "El horario de atencion es de 09:00 a 18:00"
        );
    }

    // Solo permitir bloques exactos de 30 minutos
    if (hora.getMinute() % 30 != 0
            || hora.getSecond() != 0
            || hora.getNano() != 0) {

        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Las citas deben ser en intervalos de 30 minutos"
        );
    }
}


@GetMapping("/horarios-disponibles")
public List<LocalDateTime> horariosDisponibles(
        @RequestParam LocalDate fecha,
        @AuthenticationPrincipal Jwt jwt) {

    obtenerDueno(jwt);

    List<LocalDateTime> disponibles = new ArrayList<>();

    // Sábado y domingo no hay atención
    if (fecha.getDayOfWeek().getValue() > 5) {
        return disponibles;
    }

    LocalDateTime ahora = LocalDateTime.now(
        ZoneId.of("America/Santiago")
    );

    LocalTime hora = LocalTime.of(9, 0);
    LocalTime cierre = LocalTime.of(18, 0);

    while (hora.isBefore(cierre)) {

        LocalDateTime horario = fecha.atTime(hora);

        // No mostrar horarios pasados
        if (horario.isAfter(ahora)) {

        boolean ocupado = repositorio
            .existsByEstadoInAndFechaHoraAfterAndFechaHoraBefore(
                List.of("PENDIENTE", "CONFIRMADA", "PROPUESTA"),
                horario.minusMinutes(30),
                horario.plusMinutes(30)
            );


            if (!ocupado) {
                disponibles.add(horario);
            }
        }

        hora = hora.plusMinutes(30);
    }

    return disponibles;
}



private void verificarHorarioLibreParaSolicitud(
        LocalDateTime fechaHora) {

    boolean ocupado = repositorio
        .existsByEstadoInAndFechaHoraAfterAndFechaHoraBefore(
            List.of("PENDIENTE", "CONFIRMADA", "PROPUESTA"),
            fechaHora.minusMinutes(30),
            fechaHora.plusMinutes(30)
        );

    if (ocupado) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "El horario ya tiene una solicitud o cita registrada"
        );
    }
}


@PatchMapping("/{id}/cancelar")
public Cita cancelarCita(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt) {

    Long duenoId = obtenerDueno(jwt);

    Cita cita = repositorio.findById(id)
        .filter(c -> c.getDuenoUsuarioId().equals(duenoId))
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Cita no encontrada"
        ));

    if (!"PENDIENTE".equals(cita.getEstado())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Solo se pueden cancelar solicitudes pendientes"
        );
    }

    cita.setEstado("CANCELADA");

    return repositorio.save(cita);
}

}
