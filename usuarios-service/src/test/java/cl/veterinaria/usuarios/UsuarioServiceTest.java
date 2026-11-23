package cl.veterinaria.usuarios;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class UsuarioServiceTest {

    private UsuarioRepository repositorio;
    private UsuarioService servicio;

    private final BCryptPasswordEncoder encoder =
        new BCryptPasswordEncoder();

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);
        servicio = new UsuarioService(repositorio);
    }

    private RegistroRequest registro(
            String rut,
            String correo,
            String password,
            String confirmarPassword) {

        return new RegistroRequest(
            rut,
            "Ana",
            "Pérez",
            correo,
            password,
            confirmarPassword,
            LocalDate.of(2000, 5, 10),
            "O'Higgins",
            "Rancagua",
            "Dirección de prueba"
        );
    }

    private Usuario usuarioExistente(
            boolean activo,
            String password) {

        Usuario usuario = new Usuario();

        usuario.setNombre("Ana");
        usuario.setCorreo("ana@ejemplo.com");
        usuario.setRut("123456785");
        usuario.setRol(RolUsuario.DUENO_MASCOTA);
        usuario.setActivo(activo);

        usuario.setContrasenaHash(
            encoder.encode(password)
        );

        return usuario;
    }

    private void simularGuardado() {
        when(repositorio.save(any(Usuario.class)))
            .thenAnswer(invocacion ->
                invocacion.getArgument(0)
            );
    }

    // PRUEBA 1
    @Test
    void registrarUsuarioCorrectamente() {

        simularGuardado();

        RegistroRequest datos = registro(
            "123456785",
            "ana@ejemplo.com",
            "ClaveSegura123!",
            "ClaveSegura123!"
        );

        Usuario resultado = servicio.registrar(datos);

        assertEquals("Ana", resultado.getNombre());

        assertEquals(
            "ana@ejemplo.com",
            resultado.getCorreo()
        );

        assertEquals(
            RolUsuario.DUENO_MASCOTA,
            resultado.getRol()
        );

        assertTrue(resultado.isActivo());

        verify(repositorio).save(any(Usuario.class));
    }

    // PRUEBA 2
    @Test
    void registrarUsuarioCifraLaContrasena() {

        simularGuardado();

        Usuario resultado = servicio.registrar(
            registro(
                "123456785",
                "ana@ejemplo.com",
                "ClaveSegura123!",
                "ClaveSegura123!"
            )
        );

        assertNotEquals(
            "ClaveSegura123!",
            resultado.getContrasenaHash()
        );

        assertTrue(
            encoder.matches(
                "ClaveSegura123!",
                resultado.getContrasenaHash()
            )
        );

        verify(repositorio).save(any(Usuario.class));
    }

    // PRUEBA 3
    @Test
    void rechazaRutInvalido() {

        RegistroRequest datos = registro(
            "123456780",
            "ana@ejemplo.com",
            "ClaveSegura123!",
            "ClaveSegura123!"
        );

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> servicio.registrar(datos)
        );

        assertEquals(
            HttpStatus.BAD_REQUEST,
            error.getStatusCode()
        );

        verify(repositorio, never())
            .save(any(Usuario.class));
    }

    // PRUEBA 4
    @Test
    void rechazaContrasenasDiferentes() {

        RegistroRequest datos = registro(
            "123456785",
            "ana@ejemplo.com",
            "ClaveSegura123!",
            "OtraClave123!"
        );

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> servicio.registrar(datos)
        );

        assertEquals(
            HttpStatus.BAD_REQUEST,
            error.getStatusCode()
        );

        verify(repositorio, never())
            .save(any(Usuario.class));
    }

    // PRUEBA 5
    @Test
    void rechazaRutDuplicado() {

        when(repositorio.existsByRut("123456785"))
            .thenReturn(true);

        RegistroRequest datos = registro(
            "123456785",
            "ana@ejemplo.com",
            "ClaveSegura123!",
            "ClaveSegura123!"
        );

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> servicio.registrar(datos)
        );

        assertEquals(
            HttpStatus.CONFLICT,
            error.getStatusCode()
        );

        verify(repositorio, never())
            .save(any(Usuario.class));
    }

    // PRUEBA 6
    @Test
    void rechazaCorreoDuplicado() {

        when(
            repositorio.existsByCorreoIgnoreCase(
                "ana@ejemplo.com"
            )
        ).thenReturn(true);

        RegistroRequest datos = registro(
            "123456785",
            "ANA@EJEMPLO.COM",
            "ClaveSegura123!",
            "ClaveSegura123!"
        );

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> servicio.registrar(datos)
        );

        assertEquals(
            HttpStatus.CONFLICT,
            error.getStatusCode()
        );

        verify(repositorio, never())
            .save(any(Usuario.class));
    }

    // PRUEBA 7
    @Test
    void permiteLoginConCredencialesCorrectas() {

        Usuario usuario = usuarioExistente(
            true,
            "ClaveSegura123!"
        );

        when(
            repositorio.findByCorreoIgnoreCase(
                "ana@ejemplo.com"
            )
        ).thenReturn(Optional.of(usuario));

        LoginRequest datos = new LoginRequest(
            "  ANA@EJEMPLO.COM  ",
            "ClaveSegura123!"
        );

        Usuario resultado =
            servicio.comprobarCredenciales(datos);

        assertNotNull(resultado);
        assertEquals("Ana", resultado.getNombre());
        assertTrue(resultado.isActivo());
    }

    // PRUEBA 8
    @Test
    void rechazaLoginConContrasenaIncorrecta() {

        Usuario usuario = usuarioExistente(
            true,
            "ClaveCorrecta123!"
        );

        when(
            repositorio.findByCorreoIgnoreCase(
                "ana@ejemplo.com"
            )
        ).thenReturn(Optional.of(usuario));

        LoginRequest datos = new LoginRequest(
            "ana@ejemplo.com",
            "ClaveIncorrecta123!"
        );

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> servicio.comprobarCredenciales(datos)
        );

        assertEquals(
            HttpStatus.UNAUTHORIZED,
            error.getStatusCode()
        );
    }

    // PRUEBA 9
    @Test
    void rechazaLoginDeUsuarioDesactivado() {

        Usuario usuario = usuarioExistente(
            false,
            "ClaveSegura123!"
        );

        when(
            repositorio.findByCorreoIgnoreCase(
                "ana@ejemplo.com"
            )
        ).thenReturn(Optional.of(usuario));

        LoginRequest datos = new LoginRequest(
            "ana@ejemplo.com",
            "ClaveSegura123!"
        );

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> servicio.comprobarCredenciales(datos)
        );

        assertEquals(
            HttpStatus.UNAUTHORIZED,
            error.getStatusCode()
        );
    }

    // PRUEBA 10
    @Test
    void rechazaLoginDeUsuarioInexistente() {

        when(
            repositorio.findByCorreoIgnoreCase(
                "noexiste@ejemplo.com"
            )
        ).thenReturn(Optional.empty());

        LoginRequest datos = new LoginRequest(
            "noexiste@ejemplo.com",
            "ClaveSegura123!"
        );

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> servicio.comprobarCredenciales(datos)
        );

        assertEquals(
            HttpStatus.UNAUTHORIZED,
            error.getStatusCode()
        );
    }
}
