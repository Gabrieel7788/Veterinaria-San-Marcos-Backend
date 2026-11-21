CREATE TABLE citas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dueno_usuario_id BIGINT NOT NULL,
    mascota_id BIGINT NOT NULL,
    servicio VARCHAR(100) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    fecha_hora DATETIME(6) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    creada_en DATETIME(6) NOT NULL,

    PRIMARY KEY (id),

    INDEX idx_citas_dueno (dueno_usuario_id),
    INDEX idx_citas_mascota (mascota_id),
    INDEX idx_citas_fecha (fecha_hora)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4;
