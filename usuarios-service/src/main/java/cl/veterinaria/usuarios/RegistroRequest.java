package cl.veterinaria.usuarios;

import java.time.LocalDate;
import jakarta.validation.constraints.*;

public record RegistroRequest(

    @NotBlank
    String rut,

    @NotBlank
    @Size(max = 50)
    String nombre,

    @NotBlank
    @Size(max = 100)
    String apellidos,

    @NotBlank
    @Email
    @Size(max = 100)
    String correo,

    @NotBlank
    @Size(min = 8, max = 72)
    String password,

    @NotBlank
    String confirmarPassword,

    LocalDate fechaNacimiento,

    @NotBlank
    String region,

    @NotBlank
    String comuna,

    @NotBlank
    @Size(max = 300)
    String direccion

) {}
