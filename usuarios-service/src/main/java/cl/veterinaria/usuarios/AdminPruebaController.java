package cl.veterinaria.usuarios;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminPruebaController {

    @GetMapping("/prueba")
    public Map<String, String> pruebaAdmin() {

        return Map.of(
            "mensaje",
            "Acceso autorizado al administrador"
        );
    }
}
