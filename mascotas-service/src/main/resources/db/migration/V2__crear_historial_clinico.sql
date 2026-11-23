CREATE TABLE atenciones_clinicas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    mascota_id BIGINT NOT NULL,
    fecha_atencion DATE NOT NULL,
    diagnostico TEXT NOT NULL,
    tratamiento TEXT,
    medicamentos TEXT,
    vacunas TEXT,
    proximo_control DATE,
    registrado_por_usuario_id BIGINT NOT NULL,
    creada_en DATETIME(6) NOT NULL,

    PRIMARY KEY (id),

    INDEX idx_atenciones_mascota (mascota_id),
    INDEX idx_atenciones_fecha (fecha_atencion),

    CONSTRAINT fk_atenciones_mascota
        FOREIGN KEY (mascota_id)
        REFERENCES mascotas(id)
        ON DELETE RESTRICT

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4;
