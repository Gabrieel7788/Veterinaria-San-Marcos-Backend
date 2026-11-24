# Veterinaria San Marcos - Backend

## Descripción

Backend del sistema web **Veterinaria San Marcos**, desarrollado para la asignatura **Desarrollo Fullstack II (DSY1104)**, Evaluación Parcial 3.

El proyecto permite gestionar usuarios, mascotas, historiales clínicos y citas veterinarias. Está desarrollado con Java y Spring Boot, utilizando una arquitectura de tres microservicios independientes.

## Tecnologías utilizadas

- Java 21 y Spring Boot
- Spring Security y JWT
- Spring Data JPA
- MySQL
- Flyway para migraciones de base de datos
- Swagger / OpenAPI
- Maven

## Microservicios

El backend está dividido en tres servicios:

| Microservicio | Puerto | Función |
|---|---|---|
| usuarios-service | 8080 | Registro, inicio de sesión y administración de usuarios |
| mascotas-service | 8081 | Gestión de mascotas, historial clínico y vacunas |
| citas-service | 8082 | Solicitud, confirmación y gestión de citas veterinarias |

Cada microservicio tiene su propia configuración y se conecta a una base de datos independiente.

## Bases de datos

Se utiliza MySQL con las siguientes bases de datos:

- `veterinaria_usuarios`
- `veterinaria_mascotas`
- `veterinaria_citas`

Los cambios en la estructura de las tablas se administran mediante **Flyway**, utilizando archivos SQL de migración.

## Seguridad y roles

La autenticación se realiza mediante tokens JWT. El backend verifica los permisos antes de permitir el acceso a las funciones protegidas.

Los roles del sistema son:

- **Administrador:** gestiona usuarios, roles y estados de las cuentas. También accede a funciones de gestión y reportes.
- **Recepcionista:** gestiona citas y registra atenciones clínicas.
- **Dueño de mascota:** administra sus mascotas, solicita citas y consulta sus historiales clínicos.

Las contraseñas de los usuarios se almacenan utilizando BCrypt.

## Requisitos para ejecutar el backend

- Java JDK 21
- Maven
- MySQL en funcionamiento
- Las tres bases de datos creadas
- Variables de entorno configuradas

## Configuración y ejecución local

**1. Iniciar MySQL**

Iniciar MySQL desde Laragon u otro entorno compatible. El proyecto utiliza el puerto `3306`.

**2. Configurar las variables de entorno**

Los tres microservicios utilizan estas variables:

| Variable | Descripción |
|---|---|
| `DB_USER` | Usuario de conexión a MySQL |
| `DB_PASSWORD` | Contraseña del usuario de MySQL |
| `JWT_SECRET_BASE64` | Clave compartida para firmar y validar tokens JWT |

La clave JWT debe ser la misma en los tres microservicios.

Por seguridad, las contraseñas y la clave JWT no se incluyen en el repositorio.

**3. Ejecutar los microservicios**

Abrir cada carpeta de servicio en el entorno de desarrollo y ejecutar su aplicación Spring Boot.

También pueden iniciarse con Maven, ejecutando `mvn spring-boot:run` dentro de cada carpeta:

- `usuarios-service`
- `mascotas-service`
- `citas-service`

Los tres servicios deben permanecer activos para utilizar todas las funciones del sistema.

## Documentación de APIs

Cada microservicio dispone de Swagger para consultar los endpoints REST disponibles.

| Servicio | Swagger |
|---|---|
| Usuarios | http://localhost:8080/swagger-ui/index.html |
| Mascotas | http://localhost:8081/swagger-ui/index.html |
| Citas | http://localhost:8082/swagger-ui/index.html |

Swagger permite revisar los métodos HTTP, las rutas y los datos que reciben las operaciones.

Estas direcciones funcionan cuando los servicios están ejecutándose localmente.

## Integración con el frontend

El frontend está desarrollado con React y TypeScript.

Se comunica con los microservicios mediante peticiones HTTP a sus APIs REST. Para acceder a los recursos protegidos, envía un token JWT que el backend valida.

**Repositorio del frontend:**

https://github.com/Gabrieel7788/Veterinaria-San-Marcos-F

## Proyecto académico

Evaluación Parcial 3 — Desarrollo Fullstack II (DSY1104).

Duoc UC.
