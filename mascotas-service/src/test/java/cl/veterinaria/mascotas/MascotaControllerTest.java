package cl.veterinaria.mascotas;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

class MascotaControllerTest {

    private MascotaRepository repositorio;
    private MascotaController controlador;

    @BeforeEach
    void preparar() {
        repositorio = mock(MascotaRepository.class);
        controlador = new MascotaController(repositorio);
    }

    private Jwt crearToken(String rol, Long usuarioId) {
        return new Jwt(
            "token-de-prueba",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "HS256"),
            Map.of(
                "sub", usuarioId.toString(),
                "rol", rol
            )
        );
    }

    private Mascota crearMascota(Long duenioId) {
        Mascota mascota = new Mascota();
        mascota.setDuenioUsuarioId(duenioId);
        mascota.setNombre("Luna");
        mascota.setEspecie("Perro");
        mascota.setRaza("Mestiza");
        return mascota;
    }

    // PRUEBA 1
    @Test
    void crearMascotaAsignaDuenoDelToken() {

        Jwt token = crearToken("DUENO_MASCOTA", 2L);

        MascotaController.MascotaDatos datos =
            new MascotaController.MascotaDatos(
                " Luna ",
                " Perro ",
                "Mestiza",
                LocalDate.of(2022, 5, 10)
            );

        when(repositorio.save(any(Mascota.class)))
            .thenAnswer(invocacion ->
                invocacion.getArgument(0)
            );

        var respuesta = controlador.crear(datos, token);

        assertEquals(
            HttpStatus.CREATED,
            respuesta.getStatusCode()
        );

        assertNotNull(respuesta.getBody());

        assertEquals(
            2L,
            respuesta.getBody().getDuenioUsuarioId()
        );

        assertEquals(
            "Luna",
            respuesta.getBody().getNombre()
        );

        verify(repositorio).save(any(Mascota.class));
    }

    // PRUEBA 2
    @Test
    void recepcionistaNoPuedeCrearMascotas() {

        Jwt token = crearToken("RECEPCIONISTA", 1L);

        MascotaController.MascotaDatos datos =
            new MascotaController.MascotaDatos(
                "Luna",
                "Perro",
                "Mestiza",
                null
            );

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.crear(datos, token)
        );

        assertEquals(
            HttpStatus.FORBIDDEN,
            error.getStatusCode()
        );

        verifyNoInteractions(repositorio);
    }

    // PRUEBA 3
    @Test
    void listarMascotasConsultaSoloLasDelDueno() {

        Jwt token = crearToken("DUENO_MASCOTA", 2L);

        Mascota luna = crearMascota(2L);

        when(
            repositorio.findByDuenioUsuarioIdOrderByIdDesc(2L)
        ).thenReturn(List.of(luna));

        List<Mascota> resultado =
            controlador.misMascotas(token);

        assertEquals(1, resultado.size());
        assertEquals("Luna", resultado.get(0).getNombre());

        verify(repositorio)
            .findByDuenioUsuarioIdOrderByIdDesc(2L);
    }

    // PRUEBA 4
    @Test
    void duenoNoPuedeVerMascotaAjena() {

        Jwt token = crearToken("DUENO_MASCOTA", 2L);

        Mascota ajena = crearMascota(99L);

        when(repositorio.findById(10L))
            .thenReturn(Optional.of(ajena));

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.obtener(10L, token)
        );

        assertEquals(
            HttpStatus.NOT_FOUND,
            error.getStatusCode()
        );
    }

    // PRUEBA 5
    @Test
    void duenoPuedeEditarSuMascota() {

        Jwt token = crearToken("DUENO_MASCOTA", 2L);

        Mascota mascota = crearMascota(2L);

        when(repositorio.findById(1L))
            .thenReturn(Optional.of(mascota));

        when(repositorio.save(any(Mascota.class)))
            .thenAnswer(invocacion ->
                invocacion.getArgument(0)
            );

        MascotaController.MascotaDatos datos =
            new MascotaController.MascotaDatos(
                "Luna Actualizada",
                "Perro",
                "Mestiza",
                null
            );

        Mascota resultado =
            controlador.editar(1L, datos, token);

        assertEquals(
            "Luna Actualizada",
            resultado.getNombre()
        );

        verify(repositorio).save(mascota);
    }

    // PRUEBA 6
    @Test
    void duenoNoPuedeEditarMascotaAjena() {

        Jwt token = crearToken("DUENO_MASCOTA", 2L);

        Mascota mascotaAjena = crearMascota(99L);

        when(repositorio.findById(10L))
            .thenReturn(Optional.of(mascotaAjena));

        MascotaController.MascotaDatos datos =
            new MascotaController.MascotaDatos(
                "Nombre cambiado",
                "Perro",
                "Mestiza",
                null
            );

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.editar(10L, datos, token)
        );

        assertEquals(
            HttpStatus.NOT_FOUND,
            error.getStatusCode()
        );

        verify(repositorio, never())
            .save(any(Mascota.class));
    }

    // PRUEBA 7
    @Test
    void duenoNoPuedeEliminarMascotaAjena() {

        Jwt token = crearToken("DUENO_MASCOTA", 2L);

        Mascota mascotaAjena = crearMascota(99L);

        when(repositorio.findById(10L))
            .thenReturn(Optional.of(mascotaAjena));

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.eliminar(10L, token)
        );

        assertEquals(
            HttpStatus.NOT_FOUND,
            error.getStatusCode()
        );

        verify(repositorio, never())
            .delete(any(Mascota.class));
    }
}
