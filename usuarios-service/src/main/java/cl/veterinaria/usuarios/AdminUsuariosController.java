package cl.veterinaria.usuarios;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuariosController {

    private final UsuarioRepository repositorio;

    public AdminUsuariosController(
            UsuarioRepository repositorio) {
        this.repositorio = repositorio;
    }

    public record UsuarioResumen(
        Long id,
        String nombre,
        String correo,
        String rol
    ) {}

    @GetMapping
    public List<UsuarioResumen> listarUsuarios(
            Authentication autenticacion) {

        boolean esAdministrador =
            autenticacion != null &&
            autenticacion.getAuthorities()
                .stream()
                .anyMatch(autoridad ->
                    "ROLE_ADMINISTRADOR".equals(
                        autoridad.getAuthority()
                    )
                );

        if (!esAdministrador) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acceso exclusivo para administradores"
            );
        }

        return repositorio.findAll()
            .stream()
            .map(usuario -> new UsuarioResumen(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                String.valueOf(usuario.getRol())
            ))
            .toList();
    }


public record CambiarRolSolicitud(String rol) {}

@PatchMapping("/{id}/rol")
public UsuarioResumen cambiarRol(
        @PathVariable Long id,
        @RequestBody CambiarRolSolicitud datos,
        Authentication autenticacion) {

    // Solo el administrador puede cambiar roles
    boolean esAdministrador =
        autenticacion != null &&
        autenticacion.getAuthorities()
            .stream()
            .anyMatch(autoridad ->
                "ROLE_ADMINISTRADOR".equals(
                    autoridad.getAuthority()
                )
            );

    if (!esAdministrador) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Acceso exclusivo para administradores"
        );
    }

    if (datos == null || datos.rol() == null ||
        !List.of("DUENO_MASCOTA", "RECEPCIONISTA")
            .contains(datos.rol())) {

        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Rol no permitido"
        );
    }

    Usuario usuario = repositorio.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Usuario no encontrado"
        ));

    // Proteger las cuentas de administrador
    if ("ADMINISTRADOR".equals(
            String.valueOf(usuario.getRol()))) {

        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "No se puede modificar el rol de un administrador"
        );
    }

    usuario.setRol(
        Enum.valueOf(
            usuario.getRol().getDeclaringClass(),
            datos.rol()
        )
    );

    repositorio.save(usuario);

    return new UsuarioResumen(
        usuario.getId(),
        usuario.getNombre(),
        usuario.getCorreo(),
        String.valueOf(usuario.getRol())
    );
}

}
