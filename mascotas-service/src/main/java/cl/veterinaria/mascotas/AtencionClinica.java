package cl.veterinaria.mascotas;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "atenciones_clinicas")
public class AtencionClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mascota_id", nullable = false)
    private Long mascotaId;

    @Column(name = "fecha_atencion", nullable = false)
    private LocalDate fechaAtencion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String diagnostico;

    @Column(columnDefinition = "TEXT")
    private String tratamiento;

    @Column(columnDefinition = "TEXT")
    private String medicamentos;

    @Column(columnDefinition = "TEXT")
    private String vacunas;

    @Column(name = "proximo_control")
    private LocalDate proximoControl;

    @Column(name = "proxima_vacuna")
    private LocalDate proximaVacuna;

    @Column(name = "registrado_por_usuario_id", nullable = false)
    private Long registradoPorUsuarioId;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    public AtencionClinica() {}

    public Long getId() {
        return id;
    }

    public Long getMascotaId() {
        return mascotaId;
    }

    public void setMascotaId(Long mascotaId) {
        this.mascotaId = mascotaId;
    }

    public LocalDate getFechaAtencion() {
        return fechaAtencion;
    }

    public void setFechaAtencion(LocalDate fechaAtencion) {
        this.fechaAtencion = fechaAtencion;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(String tratamiento) {
        this.tratamiento = tratamiento;
    }

    public String getMedicamentos() {
        return medicamentos;
    }

    public void setMedicamentos(String medicamentos) {
        this.medicamentos = medicamentos;
    }

    public String getVacunas() {
        return vacunas;
    }

    public void setVacunas(String vacunas) {
        this.vacunas = vacunas;
    }

    public LocalDate getProximoControl() {
        return proximoControl;
    }

    public void setProximoControl(LocalDate proximoControl) {
        this.proximoControl = proximoControl;
    }

    public Long getRegistradoPorUsuarioId() {
        return registradoPorUsuarioId;
    }

    public void setRegistradoPorUsuarioId(Long id) {
        this.registradoPorUsuarioId = id;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public void setCreadaEn(LocalDateTime creadaEn) {
        this.creadaEn = creadaEn;
    }

    public LocalDate getProximaVacuna() {
        return proximaVacuna;
    }

    public void setProximaVacuna(LocalDate proximaVacuna) {
        this.proximaVacuna = proximaVacuna;
    }

}
