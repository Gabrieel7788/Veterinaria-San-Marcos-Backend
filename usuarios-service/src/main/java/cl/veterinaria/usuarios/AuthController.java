package cl.veterinaria.usuarios;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public AuthController(
            UsuarioService usuarioService,
            JwtService jwtService) {

        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginRespuesta> login(
            @Valid @RequestBody LoginRequest datos) {

        Usuario usuario =
                usuarioService.comprobarCredenciales(datos);

        String token = jwtService.generarToken(usuario);

        LoginRespuesta respuesta = new LoginRespuesta(
            token,
            "Bearer",
            1800,
            usuario.getId(),
            usuario.getNombre(),
            usuario.getCorreo(),
            usuario.getRol().name()
        );

        return ResponseEntity.ok(respuesta);
    }

    public record LoginRespuesta(
        String token,
        String tipo,
        int expiraEnSegundos,
        Long id,
        String nombre,
        String correo,
        String rol
    ) {}
}
