package cl.veterinaria.mascotas;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

class HistorialClinicoControllerTest {

    private MascotaRepository mascotas;
    private AtencionClinicaRepository historial;
    private HistorialClinicoController controlador;

    @BeforeEach
    void preparar() {
        mascotas = mock(MascotaRepository.class);
        historial = mock(AtencionClinicaRepository.class);

        controlador = new HistorialClinicoController(
            mascotas,
            historial
        );
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

    private AtencionClinica crearAtencion(Long mascotaId) {
        AtencionClinica atencion = new AtencionClinica();

        atencion.setMascotaId(mascotaId);
        atencion.setFechaAtencion(LocalDate.now());
        atencion.setDiagnostico("Control general");
        atencion.setTratamiento("Reposo");
        atencion.setMedicamentos("Ninguno");
        atencion.setVacunas("Sin vacunas");
        atencion.setRegistradoPorUsuarioId(1L);
        atencion.setCreadaEn(LocalDateTime.now());

        return atencion;
    }

    // PRUEBA 1
    @Test
    void duenioPuedeConsultarHistorialDeSuMascota() {

        Jwt token = crearToken("DUENO_MASCOTA", 2L);

        when(mascotas.findById(1L))
            .thenReturn(Optional.of(crearMascota(2L)));

        when(
            historial.findByMascotaIdOrderByFechaAtencionDescIdDesc(1L)
        ).thenReturn(List.of(crearAtencion(1L)));

        List<AtencionClinica> resultado =
            controlador.consultarHistorial(1L, token);

        assertEquals(1, resultado.size());

        assertEquals(
            "Control general",
            resultado.get(0).getDiagnostico()
        );
    }

    // PRUEBA 2
    @Test
    void duenioNoPuedeConsultarHistorialAjeno() {

        Jwt token = crearToken("DUENO_MASCOTA", 2L);

        when(mascotas.findById(10L))
            .thenReturn(Optional.of(crearMascota(99L)));

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.consultarHistorial(10L, token)
        );

        assertEquals(
            HttpStatus.NOT_FOUND,
            error.getStatusCode()
        );

        verifyNoInteractions(historial);
    }

    // PRUEBA 3
    @Test
    void recepcionistaPuedeConsultarHistorial() {

        Jwt token = crearToken("RECEPCIONISTA", 1L);

        when(mascotas.findById(1L))
            .thenReturn(Optional.of(crearMascota(2L)));

        when(
            historial.findByMascotaIdOrderByFechaAtencionDescIdDesc(1L)
        ).thenReturn(List.of(crearAtencion(1L)));

        List<AtencionClinica> resultado =
            controlador.consultarHistorial(1L, token);

        assertEquals(1, resultado.size());

        verify(historial)
            .findByMascotaIdOrderByFechaAtencionDescIdDesc(1L);
    }

    // PRUEBA 4
    @Test
    void duenioNoPuedeRegistrarAtenciones() {

        Jwt token = crearToken("DUENO_MASCOTA", 2L);

        var datos = new HistorialClinicoController.DatosAtencion(
            LocalDate.now(),
            "Diagnóstico de prueba",
            "Tratamiento",
            "Ninguno",
            null,
            null,
            null
        );

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.registrarAtencion(
                1L,
                datos,
                token
            )
        );

        assertEquals(
            HttpStatus.FORBIDDEN,
            error.getStatusCode()
        );

        verify(historial, never())
            .save(any(AtencionClinica.class));
    }

    // PRUEBA 5
    @Test
    void recepcionistaPuedeRegistrarAtencion() {

        Jwt token = crearToken("RECEPCIONISTA", 1L);

        when(mascotas.findById(1L))
            .thenReturn(Optional.of(crearMascota(2L)));

        when(historial.save(any(AtencionClinica.class)))
            .thenAnswer(invocacion ->
                invocacion.getArgument(0)
            );

        var datos = new HistorialClinicoController.DatosAtencion(
            LocalDate.now(),
            "Control preventivo",
            "Revisión general",
            "Ninguno",
            "Vacuna de prueba",
            LocalDate.now().plusDays(30),
            LocalDate.now().plusDays(60)
        );

        var respuesta = controlador.registrarAtencion(
            1L,
            datos,
            token
        );

        assertEquals(
            HttpStatus.CREATED,
            respuesta.getStatusCode()
        );

        AtencionClinica guardada = respuesta.getBody();

        assertNotNull(guardada);
        assertEquals(1L, guardada.getMascotaId());

        assertEquals(
            "Control preventivo",
            guardada.getDiagnostico()
        );

        assertEquals(
            1L,
            guardada.getRegistradoPorUsuarioId()
        );

        assertEquals(
            LocalDate.now().plusDays(60),
            guardada.getProximaVacuna()
        );

        verify(historial).save(any(AtencionClinica.class));
    }

    // PRUEBA 6
    @Test
    void noPermiteProximaVacunaSinIndicarVacuna() {

        Jwt token = crearToken("RECEPCIONISTA", 1L);

        when(mascotas.findById(1L))
            .thenReturn(Optional.of(crearMascota(2L)));

        var datos = new HistorialClinicoController.DatosAtencion(
            LocalDate.now(),
            "Control preventivo",
            "Sin tratamiento",
            "Ninguno",
            null,
            null,
            LocalDate.now().plusDays(30)
        );

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.registrarAtencion(
                1L,
                datos,
                token
            )
        );

        assertEquals(
            HttpStatus.BAD_REQUEST,
            error.getStatusCode()
        );

        verify(historial, never())
            .save(any(AtencionClinica.class));
    }

    // PRUEBA 7
    @Test
    void duenioNoPuedeEditarAtenciones() {

        Jwt token = crearToken("DUENO_MASCOTA", 2L);

        var datos = new HistorialClinicoController.DatosEdicion(
            LocalDate.now(),
            "Diagnóstico cambiado",
            "Tratamiento",
            "Ninguno",
            null,
            null,
            null
        );

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.editarAtencion(
                1L,
                5L,
                datos,
                token
            )
        );

        assertEquals(
            HttpStatus.FORBIDDEN,
            error.getStatusCode()
        );

        verify(historial, never())
            .save(any(AtencionClinica.class));
    }

    // PRUEBA 8
    @Test
    void recepcionistaPuedeEditarAtencion() {

        Jwt token = crearToken("RECEPCIONISTA", 1L);

        AtencionClinica existente = crearAtencion(1L);

        when(mascotas.findById(1L))
            .thenReturn(Optional.of(crearMascota(2L)));

        when(historial.findById(5L))
            .thenReturn(Optional.of(existente));

        when(historial.save(any(AtencionClinica.class)))
            .thenAnswer(invocacion ->
                invocacion.getArgument(0)
            );

        var datos = new HistorialClinicoController.DatosEdicion(
            LocalDate.now(),
            "Diagnóstico actualizado",
            "Nuevo tratamiento",
            "Ninguno",
            "Vacuna de prueba",
            null,
            LocalDate.now().plusDays(90)
        );

        AtencionClinica resultado =
            controlador.editarAtencion(
                1L,
                5L,
                datos,
                token
            );

        assertEquals(
            "Diagnóstico actualizado",
            resultado.getDiagnostico()
        );

        assertEquals(
            "Nuevo tratamiento",
            resultado.getTratamiento()
        );

        assertEquals(
            1L,
            resultado.getRegistradoPorUsuarioId()
        );

        verify(historial).save(existente);
    }

    // PRUEBA 9
    @Test
    void noPermiteEditarAtencionDeOtraMascota() {

        Jwt token = crearToken("ADMINISTRADOR", 3L);

        // Se solicita la mascota 1.
        when(mascotas.findById(1L))
            .thenReturn(Optional.of(crearMascota(2L)));

        // Pero la atención pertenece a la mascota 10.
        when(historial.findById(5L))
            .thenReturn(Optional.of(crearAtencion(10L)));

        var datos = new HistorialClinicoController.DatosEdicion(
            LocalDate.now(),
            "Intento de modificación",
            null,
            null,
            null,
            null,
            null
        );

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.editarAtencion(
                1L,
                5L,
                datos,
                token
            )
        );

        assertEquals(
            HttpStatus.NOT_FOUND,
            error.getStatusCode()
        );

        verify(historial, never())
            .save(any(AtencionClinica.class));
    }
}
