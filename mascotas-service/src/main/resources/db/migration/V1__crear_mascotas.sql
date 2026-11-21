CREATE TABLE mascotas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    duenio_usuario_id BIGINT NOT NULL,
    nombre VARCHAR(80) NOT NULL,
    especie VARCHAR(40) NOT NULL,
    raza VARCHAR(80),
    fecha_nacimiento DATE,

    PRIMARY KEY (id),

    INDEX idx_mascotas_duenio (duenio_usuario_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4;
