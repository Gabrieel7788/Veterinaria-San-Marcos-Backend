package cl.veterinaria.mascotas;

import java.time.LocalDate;
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
public class MascotaController {

    private final MascotaRepository repositorio;

    public MascotaController(MascotaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public record MascotaDatos(
        @NotBlank @Size(max = 80) String nombre,
        @NotBlank @Size(max = 40) String especie,
        @Size(max = 80) String raza,
        @PastOrPresent LocalDate fechaNacimiento
    ) {}

    private Long obtenerDueno(Jwt jwt) {

        if (!"DUENO_MASCOTA".equals(
                jwt.getClaimAsString("rol"))) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Operacion disponible para duenos"
            );
        }

        return Long.valueOf(jwt.getSubject());
    }

    private Mascota buscarPropia(Long id, Long duenoId) {

        return repositorio.findById(id)
            .filter(m -> m.getDuenioUsuarioId().equals(duenoId))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Mascota no encontrada"
            ));
    }

    private void actualizar(
            Mascota mascota, MascotaDatos datos) {

        mascota.setNombre(datos.nombre().trim());
        mascota.setEspecie(datos.especie().trim());
        mascota.setRaza(datos.raza());
        mascota.setFechaNacimiento(datos.fechaNacimiento());
    }

    @PostMapping
    public ResponseEntity<Mascota> crear(
            @Valid @RequestBody MascotaDatos datos,
            @AuthenticationPrincipal Jwt jwt) {

        Long duenoId = obtenerDueno(jwt);

        Mascota mascota = new Mascota();
        mascota.setDuenioUsuarioId(duenoId);
        actualizar(mascota, datos);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(repositorio.save(mascota));
    }

    @GetMapping("/mias")
    public List<Mascota> misMascotas(
            @AuthenticationPrincipal Jwt jwt) {

        Long duenoId = obtenerDueno(jwt);

        return repositorio
            .findByDuenioUsuarioIdOrderByIdDesc(duenoId);
    }

    @GetMapping("/{id}")
    public Mascota obtener(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        return buscarPropia(id, obtenerDueno(jwt));
    }

    @PutMapping("/{id}")
    public Mascota editar(
            @PathVariable Long id,
            @Valid @RequestBody MascotaDatos datos,
            @AuthenticationPrincipal Jwt jwt) {

        Mascota mascota = buscarPropia(
            id, obtenerDueno(jwt)
        );

        actualizar(mascota, datos);

        return repositorio.save(mascota);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        Mascota mascota = buscarPropia(
            id, obtenerDueno(jwt)
        );

        repositorio.delete(mascota);

        return ResponseEntity.noContent().build();
    }
}
