package cl.veterinaria.usuarios;

import java.io.IOException;
import java.util.List;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtFiltro extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository repositorio;

    public JwtFiltro(
            JwtService jwtService,
            UsuarioRepository repositorio) {

        this.jwtService = jwtService;
        this.repositorio = repositorio;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String encabezado = request.getHeader("Authorization");

        if (encabezado != null &&
                encabezado.startsWith("Bearer ")) {

            String token = encabezado.substring(7);

            try {
                Long id = jwtService.validarYObtenerId(token);

                repositorio.findById(id)
                    .filter(Usuario::isActivo)
                    .ifPresent(usuario -> {

                        var permisos = List.of(
                            new SimpleGrantedAuthority(
                                "ROLE_" + usuario.getRol().name()
                            )
                        );

                        var autenticacion =
                            new UsernamePasswordAuthenticationToken(
                                usuario.getId(),
                                null,
                                permisos
                            );

                        SecurityContextHolder.getContext()
                            .setAuthentication(autenticacion);
                    });

            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
