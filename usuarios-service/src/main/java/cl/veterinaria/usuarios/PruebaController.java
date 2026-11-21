package cl.veterinaria.usuarios;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PruebaController {

    @GetMapping("/api/prueba")
    public Map<String, String> comprobarServidor() {
        return Map.of(
            "mensaje",
            "Backend de Veterinaria San Marcos funcionando"
        );
    }
}
