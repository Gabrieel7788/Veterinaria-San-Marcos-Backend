package cl.veterinaria.usuarios;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class RenovarTokenController {

    private final UsuarioRepository repositorio;
    private final JwtService jwtService;

    public RenovarTokenController(
            UsuarioRepository repositorio,
            JwtService jwtService) {
        this.repositorio = repositorio;
        this.jwtService = jwtService;
    }

    public record RespuestaRenovacion(
        String token,
        String tipo,
        int expiraEnSegundos
    ) {}

    @PostMapping("/renovar")
    public ResponseEntity<RespuestaRenovacion> renovar(
            Authentication autenticacion) {

        if (autenticacion == null ||
                !autenticacion.isAuthenticated() ||
                !(autenticacion.getPrincipal() instanceof Long id)) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesion"
            );
        }

        Usuario usuario = repositorio.findById(id)
            .filter(Usuario::isActivo)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autorizado"
            ));

        String nuevoToken = jwtService.generarToken(usuario);

        return ResponseEntity.ok(
            new RespuestaRenovacion(
                nuevoToken,
                "Bearer",
                1800
            )
        );
    }
}
