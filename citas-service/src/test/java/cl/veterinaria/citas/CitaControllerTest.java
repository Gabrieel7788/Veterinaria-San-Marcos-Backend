package cl.veterinaria.citas;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

class CitaControllerTest {

    private CitaRepository repositorio;
    private CitaController controlador;

    @BeforeEach
    void preparar() {
        repositorio = mock(CitaRepository.class);
        controlador = new CitaController(repositorio);
    }

    private Jwt token(String rol, Long usuarioId) {
        return new Jwt(
            "token-prueba",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "HS256"),
            Map.of(
                "sub", usuarioId.toString(),
                "rol", rol
            )
        );
    }

    // Siempre utiliza un lunes futuro.
    private LocalDateTime fechaFutura(int hora, int minuto) {
        return LocalDate.now(ZoneId.of("America/Santiago"))
            .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            .atTime(hora, minuto);
    }

    private Cita cita(
            Long duenoId,
            String estado,
            LocalDateTime fecha) {

        Cita c = new Cita();
        c.setDuenoUsuarioId(duenoId);
        c.setMascotaId(1L);
        c.setServicio("Consulta general");
        c.setMotivo("Control");
        c.setFechaHora(fecha);
        c.setEstado(estado);

        return c;
    }

    private void simularGuardado() {
        when(repositorio.save(any(Cita.class)))
            .thenAnswer(invocacion ->
                invocacion.getArgument(0)
            );
    }

    // PRUEBA 1
    @Test
    void duenoPuedeConsultarSusCitas() {

        Jwt jwt = token("DUENO_MASCOTA", 2L);

        Cita existente = cita(
            2L, "PENDIENTE", fechaFutura(10, 0)
        );

        when(repositorio.findByDuenoUsuarioIdOrderByIdDesc(2L))
            .thenReturn(List.of(existente));

        List<Cita> resultado = controlador.misCitas(jwt);

        assertEquals(1, resultado.size());
        assertEquals(2L, resultado.get(0).getDuenoUsuarioId());

        verify(repositorio)
            .findByDuenoUsuarioIdOrderByIdDesc(2L);
    }

    // PRUEBA 2
    @Test
    void recepcionistaNoPuedeConsultarMisCitas() {

        Jwt jwt = token("RECEPCIONISTA", 1L);

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.misCitas(jwt)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(repositorio);
    }

    // PRUEBA 3
    @Test
    void recepcionistaPuedeConsultarTodasLasCitas() {

        Jwt jwt = token("RECEPCIONISTA", 1L);

        when(repositorio.findAll()).thenReturn(
            List.of(cita(
                2L, "PENDIENTE", fechaFutura(10, 0)
            ))
        );

        List<Cita> resultado =
            controlador.listarTodasLasCitas(jwt);

        assertEquals(1, resultado.size());
        verify(repositorio).findAll();
    }

    // PRUEBA 4
    @Test
    void duenoNoPuedeConsultarCitasDeRecepcion() {

        Jwt jwt = token("DUENO_MASCOTA", 2L);

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.listarTodasLasCitas(jwt)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(repositorio);
    }

    // PRUEBA 5
    @Test
    void recepcionistaPuedeConfirmarCitaPendiente() {

        Jwt jwt = token("RECEPCIONISTA", 1L);

        Cita existente = cita(
            2L, "PENDIENTE", fechaFutura(10, 0)
        );

        when(repositorio.findById(5L))
            .thenReturn(Optional.of(existente));

        simularGuardado();

        Cita resultado = controlador.confirmarCita(5L, jwt);

        assertEquals("CONFIRMADA", resultado.getEstado());
        verify(repositorio).save(existente);
    }

    // PRUEBA 6
    @Test
    void duenoNoPuedeConfirmarCitas() {

        Jwt jwt = token("DUENO_MASCOTA", 2L);

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.confirmarCita(5L, jwt)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(repositorio);
    }

    // PRUEBA 7
    @Test
    void noPuedeConfirmarHorarioOcupado() {

        Jwt jwt = token("RECEPCIONISTA", 1L);

        Cita existente = cita(
            2L, "PENDIENTE", fechaFutura(10, 0)
        );

        when(repositorio.findById(5L))
            .thenReturn(Optional.of(existente));

        when(
            repositorio.existsByEstadoAndFechaHoraAfterAndFechaHoraBefore(
                eq("CONFIRMADA"),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
            )
        ).thenReturn(true);

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.confirmarCita(5L, jwt)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());

        verify(repositorio, never())
            .save(any(Cita.class));
    }

    // PRUEBA 8
    @Test
    void noPuedeConfirmarCitaQueYaEstaConfirmada() {

        Jwt jwt = token("RECEPCIONISTA", 1L);

        when(repositorio.findById(5L))
            .thenReturn(Optional.of(
                cita(2L, "CONFIRMADA", fechaFutura(10, 0))
            ));

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.confirmarCita(5L, jwt)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());

        verify(repositorio, never())
            .save(any(Cita.class));
    }

    // PRUEBA 9
    @Test
    void recepcionistaPuedeProponerOtroHorario() {

        Jwt jwt = token("RECEPCIONISTA", 1L);

        Cita existente = cita(
            2L, "PENDIENTE", fechaFutura(10, 0)
        );

        LocalDateTime nuevoHorario = fechaFutura(11, 0);

        when(repositorio.findById(5L))
            .thenReturn(Optional.of(existente));

        simularGuardado();

        var datos = new CitaController.PropuestaHorario(
            nuevoHorario
        );

        Cita resultado = controlador.proponerHorario(
            5L, datos, jwt
        );

        assertEquals("PROPUESTA", resultado.getEstado());
        assertEquals(
            nuevoHorario,
            resultado.getFechaHoraPropuesta()
        );

        verify(repositorio).save(existente);
    }

    // PRUEBA 10
    @Test
    void duenoPuedeAceptarSuPropuesta() {

        Jwt jwt = token("DUENO_MASCOTA", 2L);

        Cita existente = cita(
            2L, "PROPUESTA", fechaFutura(10, 0)
        );

        LocalDateTime nuevaFecha = fechaFutura(11, 0);
        existente.setFechaHoraPropuesta(nuevaFecha);

        when(repositorio.findById(5L))
            .thenReturn(Optional.of(existente));

        simularGuardado();

        Cita resultado = controlador.aceptarPropuesta(
            5L, jwt
        );

        assertEquals("CONFIRMADA", resultado.getEstado());
        assertEquals(nuevaFecha, resultado.getFechaHora());
        assertNull(resultado.getFechaHoraPropuesta());
    }

    // PRUEBA 11
    @Test
    void duenoNoPuedeAceptarPropuestaAjena() {

        Jwt jwt = token("DUENO_MASCOTA", 2L);

        Cita ajena = cita(
            99L, "PROPUESTA", fechaFutura(10, 0)
        );

        ajena.setFechaHoraPropuesta(fechaFutura(11, 0));

        when(repositorio.findById(5L))
            .thenReturn(Optional.of(ajena));

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.aceptarPropuesta(5L, jwt)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());

        verify(repositorio, never())
            .save(any(Cita.class));
    }

    // PRUEBA 12
    @Test
    void duenoPuedeCancelarSolicitudPendiente() {

        Jwt jwt = token("DUENO_MASCOTA", 2L);

        Cita existente = cita(
            2L, "PENDIENTE", fechaFutura(10, 0)
        );

        when(repositorio.findById(5L))
            .thenReturn(Optional.of(existente));

        simularGuardado();

        Cita resultado = controlador.cancelarCita(5L, jwt);

        assertEquals("CANCELADA", resultado.getEstado());
        verify(repositorio).save(existente);
    }

    // PRUEBA 13
    @Test
    void duenoNoPuedeCancelarCitaAjena() {

        Jwt jwt = token("DUENO_MASCOTA", 2L);

        when(repositorio.findById(5L))
            .thenReturn(Optional.of(
                cita(99L, "PENDIENTE", fechaFutura(10, 0))
            ));

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.cancelarCita(5L, jwt)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());

        verify(repositorio, never())
            .save(any(Cita.class));
    }

    // PRUEBA 14
    @Test
    void noPuedeCancelarCitaConfirmada() {

        Jwt jwt = token("DUENO_MASCOTA", 2L);

        when(repositorio.findById(5L))
            .thenReturn(Optional.of(
                cita(2L, "CONFIRMADA", fechaFutura(10, 0))
            ));

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controlador.cancelarCita(5L, jwt)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());

        verify(repositorio, never())
            .save(any(Cita.class));
    }

    // PRUEBA 15
    @Test
    void noHayHorariosDisponiblesLosSabados() {

        Jwt jwt = token("DUENO_MASCOTA", 2L);

        LocalDate sabado = LocalDate
            .now(ZoneId.of("America/Santiago"))
            .with(TemporalAdjusters.next(DayOfWeek.SATURDAY));

        List<LocalDateTime> resultado =
            controlador.horariosDisponibles(sabado, jwt);

        assertTrue(resultado.isEmpty());

        verifyNoInteractions(repositorio);
    }
}
