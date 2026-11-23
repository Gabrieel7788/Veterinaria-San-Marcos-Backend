package cl.veterinaria.mascotas;

import java.time.LocalDate;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/mascotas")
public class HistorialClinicoController {

    private final MascotaRepository mascotas;
    private final AtencionClinicaRepository historial;

    public HistorialClinicoController(
            MascotaRepository mascotas,
            AtencionClinicaRepository historial) {

        this.mascotas = mascotas;
        this.historial = historial;
    }

    public record DatosAtencion(
        @NotNull @PastOrPresent
        LocalDate fechaAtencion,

        @NotBlank @Size(max = 2000)
        String diagnostico,

        @Size(max = 2000)
        String tratamiento,

        @Size(max = 2000)
        String medicamentos,

        @Size(max = 2000)
        String vacunas,

        @FutureOrPresent
        LocalDate proximoControl,

        @FutureOrPresent
        LocalDate proximaVacuna

    ) {}

    private boolean esPersonal(String rol) {
        return "RECEPCIONISTA".equals(rol)
            || "ADMINISTRADOR".equals(rol);
    }

    private Mascota buscarMascota(Long id) {
        return mascotas.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Mascota no encontrada"
            ));
    }

    // Consultar historial clínico
    @GetMapping("/{id}/historial")
    public List<AtencionClinica> consultarHistorial(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        Mascota mascota = buscarMascota(id);

        String rol = jwt.getClaimAsString("rol");

        if ("DUENO_MASCOTA".equals(rol)) {

            Long usuarioId = Long.valueOf(
                jwt.getSubject()
            );

            if (!mascota.getDuenioUsuarioId()
                    .equals(usuarioId)) {

                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Mascota no encontrada"
                );
            }

        } else if (!esPersonal(rol)) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tienes permiso para ver el historial"
            );
        }

        return historial
            .findByMascotaIdOrderByFechaAtencionDescIdDesc(id);
    }

    // Registrar una nueva atención clínica
    @PostMapping("/{id}/historial")
    public ResponseEntity<AtencionClinica> registrarAtencion(
            @PathVariable Long id,
            @Valid @RequestBody DatosAtencion datos,
            @AuthenticationPrincipal Jwt jwt) {

        String rol = jwt.getClaimAsString("rol");

        if (!esPersonal(rol)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo el personal puede registrar atenciones"
            );
        }

        buscarMascota(id);

        if (datos.proximaVacuna() != null &&
                (datos.vacunas() == null ||
                datos.vacunas().isBlank())) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debes indicar la vacuna para registrar su proxima fecha"
            );
        }

        AtencionClinica atencion = new AtencionClinica();

        atencion.setMascotaId(id);
        atencion.setFechaAtencion(datos.fechaAtencion());
        atencion.setDiagnostico(datos.diagnostico().trim());
        atencion.setTratamiento(datos.tratamiento());
        atencion.setMedicamentos(datos.medicamentos());
        atencion.setVacunas(datos.vacunas());
        atencion.setProximoControl(datos.proximoControl());
        atencion.setProximaVacuna(datos.proximaVacuna());

        atencion.setRegistradoPorUsuarioId(
            Long.valueOf(jwt.getSubject())
        );

        atencion.setCreadaEn(
            LocalDateTime.now(
                ZoneId.of("America/Santiago")
            )
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(historial.save(atencion));
    }


@GetMapping("/recepcion")
public List<Mascota> listarMascotasRecepcion(
        @AuthenticationPrincipal Jwt jwt) {

    String rol = jwt.getClaimAsString("rol");

    if (!esPersonal(rol)) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "No tienes permiso para consultar las fichas"
        );
    }

    return mascotas.findAll();
}


public record ReporteAtenciones(
    LocalDate desde,
    LocalDate hasta,
    long totalAtenciones,
    long mascotasAtendidas,
    List<AtencionClinica> detalle
) {}

@GetMapping("/reportes/atenciones")
public ReporteAtenciones generarReporte(
        @RequestParam LocalDate desde,
        @RequestParam LocalDate hasta,
        @AuthenticationPrincipal Jwt jwt) {

    String rol = jwt.getClaimAsString("rol");

    if (!"ADMINISTRADOR".equals(rol)) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Solo el administrador puede consultar reportes"
        );
    }

    if (hasta.isBefore(desde)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "La fecha final no puede ser anterior a la inicial"
        );
    }

    List<AtencionClinica> atenciones = historial
        .findByFechaAtencionBetweenOrderByFechaAtencionDescIdDesc(
            desde,
            hasta
        );

    long mascotasAtendidas = atenciones.stream()
        .map(AtencionClinica::getMascotaId)
        .distinct()
        .count();

    return new ReporteAtenciones(
        desde,
        hasta,
        atenciones.size(),
        mascotasAtendidas,
        atenciones
    );
}


public record DatosEdicion(
    @NotNull @PastOrPresent
    LocalDate fechaAtencion,

    @NotBlank @Size(max = 2000)
    String diagnostico,

    @Size(max = 2000)
    String tratamiento,

    @Size(max = 2000)
    String medicamentos,

    @Size(max = 2000)
    String vacunas,

    LocalDate proximoControl,

    LocalDate proximaVacuna
) {}

@PutMapping("/{mascotaId}/historial/{atencionId}")
public AtencionClinica editarAtencion(
        @PathVariable Long mascotaId,
        @PathVariable Long atencionId,
        @Valid @RequestBody DatosEdicion datos,
        @AuthenticationPrincipal Jwt jwt) {

    String rol = jwt.getClaimAsString("rol");

    if (!esPersonal(rol)) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Solo el personal puede editar atenciones"
        );
    }

    buscarMascota(mascotaId);

    AtencionClinica atencion = historial
        .findById(atencionId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Atencion clinica no encontrada"
        ));

    // Evitar editar una atencion de otra mascota.
    if (!atencion.getMascotaId().equals(mascotaId)) {
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "La atencion no corresponde a esta mascota"
        );
    }

    if (datos.proximaVacuna() != null &&
            (datos.vacunas() == null ||
             datos.vacunas().isBlank())) {

        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Debes indicar la vacuna correspondiente"
        );
    }

    atencion.setFechaAtencion(datos.fechaAtencion());
    atencion.setDiagnostico(datos.diagnostico().trim());
    atencion.setTratamiento(datos.tratamiento());
    atencion.setMedicamentos(datos.medicamentos());
    atencion.setVacunas(datos.vacunas());
    atencion.setProximoControl(datos.proximoControl());
    atencion.setProximaVacuna(datos.proximaVacuna());

    return historial.save(atencion);
}

}
