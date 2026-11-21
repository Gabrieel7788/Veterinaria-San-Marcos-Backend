package cl.veterinaria.usuarios;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:5173")
public class RegistroController {


    private final UsuarioService usuarioService;

    public RegistroController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    public ResponseEntity<RegistroRespuesta> registrar(
            @Valid @RequestBody RegistroRequest datos) {

        Usuario usuario = usuarioService.registrar(datos);

        RegistroRespuesta respuesta = new RegistroRespuesta(
            usuario.getId(),
            usuario.getCorreo(),
            usuario.getRol().name()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    public record RegistroRespuesta(
        Long id,
        String correo,
        String rol
    ) {}
}
