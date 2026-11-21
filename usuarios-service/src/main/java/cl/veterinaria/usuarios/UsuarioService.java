package cl.veterinaria.usuarios;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public UsuarioService(UsuarioRepository repositorio) {
        this.repositorio = repositorio;
    }

    public Usuario comprobarCredenciales(LoginRequest datos) {

        String correo = datos.correo().trim()
                .toLowerCase(Locale.ROOT);

        Usuario usuario = repositorio
                .findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Correo o contrasena incorrectos"
                ));

        if (!usuario.isActivo()) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Correo o contrasena incorrectos"
            );
        }

        boolean contrasenaCorrecta = passwordEncoder.matches(
            datos.password(),
            usuario.getContrasenaHash()
        );

        if (!contrasenaCorrecta) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Correo o contrasena incorrectos"
            );
        }

        return usuario;
    }

    public Usuario registrar(RegistroRequest datos) {

        String rut = datos.rut().trim().toUpperCase(Locale.ROOT);
        String correo = datos.correo().trim().toLowerCase(Locale.ROOT);

        if (!validarRut(rut)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "RUT invalido"
            );
        }

        if (!datos.password().equals(datos.confirmarPassword())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las contrasenas no coinciden"
            );
        }

        if (datos.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La contrasena es demasiado larga"
            );
        }

        if (repositorio.existsByRut(rut)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El RUT ya esta registrado"
            );
        }

        if (repositorio.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El correo ya esta registrado"
            );
        }

        Usuario usuario = new Usuario();

        usuario.setRut(rut);
        usuario.setNombre(datos.nombre().trim());
        usuario.setApellidos(datos.apellidos().trim());
        usuario.setCorreo(correo);

        usuario.setContrasenaHash(
            passwordEncoder.encode(datos.password())
        );

        // El registro publico nunca asigna privilegios.
        usuario.setRol(RolUsuario.DUENO_MASCOTA);
        usuario.setActivo(true);

        usuario.setRegion(datos.region().trim());
        usuario.setComuna(datos.comuna().trim());
        usuario.setDireccion(datos.direccion().trim());
        usuario.setFechaNacimiento(datos.fechaNacimiento());

        return repositorio.save(usuario);
    }

    private boolean validarRut(String rut) {
        if (!rut.matches("\\d{7,8}[0-9K]")) {
            return false;
        }

        String cuerpo = rut.substring(0, rut.length() - 1);
        int suma = 0;
        int multiplicador = 2;

        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += (cuerpo.charAt(i) - '0') * multiplicador;
            multiplicador = multiplicador == 7
                    ? 2 : multiplicador + 1;
        }

        int resultado = 11 - (suma % 11);

        String dv = resultado == 11 ? "0"
                : resultado == 10 ? "K"
                : String.valueOf(resultado);

        return rut.endsWith(dv);
    }
}
