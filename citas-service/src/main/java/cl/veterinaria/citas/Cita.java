package cl.veterinaria.citas;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dueno_usuario_id", nullable = false)
    private Long duenoUsuarioId;

    @Column(name = "mascota_id", nullable = false)
    private Long mascotaId;

    @Column(nullable = false, length = 100)
    private String servicio;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "fecha_hora_propuesta")
    private LocalDateTime fechaHoraPropuesta;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    public Cita() {}

    public Long getId() {
        return id;
    }

    public Long getDuenoUsuarioId() {
        return duenoUsuarioId;
    }

    public void setDuenoUsuarioId(Long id) {
        this.duenoUsuarioId = id;
    }

    public Long getMascotaId() {
        return mascotaId;
    }

    public void setMascotaId(Long mascotaId) {
        this.mascotaId = mascotaId;
    }

    public String getServicio() {
        return servicio;
    }

    public void setServicio(String servicio) {
        this.servicio = servicio;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public void setCreadaEn(LocalDateTime creadaEn) {
        this.creadaEn = creadaEn;
    }

    public LocalDateTime getFechaHoraPropuesta() {
        return fechaHoraPropuesta;
    }

    public void setFechaHoraPropuesta(
            LocalDateTime fechaHoraPropuesta) {
        this.fechaHoraPropuesta = fechaHoraPropuesta;
    }

}
