package cl.veterinaria.usuarios;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuariosController {


private final UsuarioRepository repositorio;
private final UsuarioService usuarioService;

public AdminUsuariosController(
        UsuarioRepository repositorio,
        UsuarioService usuarioService) {

    this.repositorio = repositorio;
    this.usuarioService = usuarioService;
}


    public record UsuarioResumen(
        Long id,
        String nombre,
        String correo,
        String rol,
        boolean activo
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
                String.valueOf(usuario.getRol()),
                usuario.isActivo()
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
        String.valueOf(usuario.getRol()),
        usuario.isActivo()
    );
}


public record CambiarEstadoSolicitud(Boolean activo) {}

@PatchMapping("/{id}/estado")
public UsuarioResumen cambiarEstado(
        @PathVariable Long id,
        @RequestBody CambiarEstadoSolicitud datos,
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

    if (datos == null || datos.activo() == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Debes indicar el estado del usuario"
        );
    }

    Usuario usuario = repositorio.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Usuario no encontrado"
        ));

    if ("ADMINISTRADOR".equals(
            String.valueOf(usuario.getRol()))) {

        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "No se puede desactivar un administrador"
        );
    }

    usuario.setActivo(datos.activo());

    repositorio.save(usuario);

    return new UsuarioResumen(
        usuario.getId(),
        usuario.getNombre(),
        usuario.getCorreo(),
        String.valueOf(usuario.getRol()),
        usuario.isActivo()
    );
}


public record EditarNombreSolicitud(String nombre) {}

@PatchMapping("/{id}/nombre")
public UsuarioResumen editarNombre(
        @PathVariable Long id,
        @RequestBody EditarNombreSolicitud datos,
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

    if (datos == null ||
        datos.nombre() == null ||
        datos.nombre().isBlank() ||
        datos.nombre().trim().length() > 100) {

        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "El nombre debe tener entre 1 y 100 caracteres"
        );
    }

    Usuario usuario = repositorio.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Usuario no encontrado"
        ));

    usuario.setNombre(datos.nombre().trim());

    repositorio.save(usuario);

    return new UsuarioResumen(
        usuario.getId(),
        usuario.getNombre(),
        usuario.getCorreo(),
        String.valueOf(usuario.getRol()),
        usuario.isActivo()
    );
}


public record CrearUsuarioAdminRequest(
    @NotNull @Valid RegistroRequest datos,
    @NotNull RolUsuario rol
) {}

@PostMapping
@Transactional
public ResponseEntity<UsuarioResumen> crearUsuario(
        @Valid @RequestBody CrearUsuarioAdminRequest solicitud) {

    // Por ahora solo creamos cuentas de
    // clientes y recepcionistas desde este formulario.
    if (solicitud.rol() != RolUsuario.DUENO_MASCOTA
            && solicitud.rol() != RolUsuario.RECEPCIONISTA) {

        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Rol no permitido para este formulario"
        );
    }

    // Reutiliza validación, BCrypt y
    // comprobación de duplicados.
    Usuario usuario = usuarioService.registrar(
        solicitud.datos()
    );

    usuario.setRol(solicitud.rol());
    usuario = repositorio.save(usuario);

    UsuarioResumen respuesta = new UsuarioResumen(
        usuario.getId(),
        usuario.getNombre(),
        usuario.getCorreo(),
        usuario.getRol().name(),
        usuario.isActivo()
    );

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(respuesta);
}

}
