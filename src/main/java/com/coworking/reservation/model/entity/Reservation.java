package com.coworking.reservation.model.entity;

import com.coworking.reservation.model.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation extends BaseEntity{
    // --- CAMPO: user (RELACIÓN) ---

    // @ManyToOne → relación "muchos a uno": MUCHAS reservas pertenecen a UN usuario.
    // Es el lado opuesto del @OneToMany(mappedBy = "user") de la clase User.
    //
    // En la base de datos, la foreign key (user_id) está en ESTA tabla (reservations),
    // no en la tabla users. El lado @ManyToOne es SIEMPRE el que tiene la foreign key.
    //
    // fetch = FetchType.LAZY:
    //   Cuando cargas una reserva, el usuario NO se carga de la BD.
    //   Hibernate crea un PROXY: un objeto que parece un User pero está vacío.
    //
    //   ¿Qué es un proxy de Hibernate?
    //   Es una subclase generada dinámicamente que extiende User.
    //   Tiene los mismos métodos, pero cuando llamas a uno (como getName()),
    //   el proxy va a la BD a buscar los datos reales en ese momento.
    //
    //   El proxy funciona SOLO dentro de una transacción activa.
    //   Si intentas acceder al proxy fuera de una transacción
    //   (por ejemplo en el controller después de que el servicio termine),
    //   obtienes LazyInitializationException.
    //
    //   ¿Por qué LAZY y no EAGER?
    //   Si fuera EAGER, cada vez que cargas UNA reserva,
    //   Hibernate también carga al usuario completo.
    //   Si cargas una LISTA de 50 reservas:
    //   - Con EAGER: 1 query para reservas + 50 queries para usuarios = 51 queries
    //   - Con LAZY: 1 query para reservas + 0 queries extra = 1 query
    //   Las queries de usuarios solo se ejecutan si accedes a sus datos.
    @ManyToOne(fetch = FetchType.LAZY)

    // @JoinColumn → especifica qué columna de la tabla "reservations"
    // contiene la foreign key hacia la tabla "users".
    //
    // name = "user_id":
    //   La columna en la tabla reservations que almacena el id del usuario.
    //   En la migración V3 definimos: user_id BIGINT NOT NULL REFERENCES users(id)
    //   Este name debe coincidir con esa columna.
    //
    // nullable = false:
    //   Toda reserva DEBE tener un usuario asociado.
    //   No pueden existir reservas huérfanas (sin dueño).
    //   Hibernate valida esto antes del INSERT.
    @JoinColumn(name = "user_id", nullable = false)

    // El tipo del campo es User (la entidad, no un id numérico).
    // En la BD se guarda como user_id (un número).
    // JPA hace la traducción automáticamente:
    //   Al guardar: lee reservation.user.id y lo pone en la columna user_id
    //   Al leer: busca el user con ese id y lo asigna al campo
    private User user;

    // --- CAMPO: room (RELACIÓN) ---
    // Misma lógica que el campo user.
    // Muchas reservas pueden ser en la MISMA sala.
    // Cada reserva es de UNA sola sala.
    //
    // mappedBy en Room.java apunta a ESTE campo:
    //   @OneToMany(mappedBy = "room") en Room.java
    //   se refiere a este "private Room room" de aquí.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // --- CAMPO: title ---
    // Título descriptivo de la reserva.
    // Ejemplos: "Reunión sprint planning", "Entrevista candidato Java"
    @Column(nullable = false, length = 200)
    private String title;

    // --- CAMPO: description ---
    // Descripción detallada opcional. Mismo uso de TEXT que en Room.
    // Sin nullable = false → es opcional.
    @Column(columnDefinition = "TEXT")
    private String description;

    // --- CAMPO: startTime ---
    // Hora de inicio de la reserva.
    //
    // LocalDateTime → fecha + hora sin zona horaria.
    // Ejemplo: 2025-01-15T10:00:00
    //
    // name = "start_time" → mapeo explícito del camelCase Java
    // al snake_case SQL. El campo Java es "startTime",
    // la columna SQL es "start_time".
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    // --- CAMPO: endTime ---
    // Hora de fin de la reserva. Debe ser POSTERIOR a startTime.
    // El CHECK constraint de la BD lo valida: CHECK (end_time > start_time)
    // Si intentas crear una reserva donde endTime <= startTime,
    // PostgreSQL rechaza el INSERT.
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // --- CAMPO: status ---
    // Estado actual de la reserva.
    // Ver ReservationStatus.java para el ciclo de vida completo.
    // @Enumerated y @Builder.Default funcionan igual que role en User.
    // Por defecto las reservas se crean como CONFIRMED.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    // --- CAMPO: version (OPTIMISTIC LOCKING) ---

    // @Version → activa el bloqueo optimista en JPA.
    //
    // ¿QUÉ PROBLEMA RESUELVE?
    //
    // Situación sin @Version:
    //   1. Admin A abre la reserva #5 en su pantalla (título: "Reunión")
    //   2. Admin B abre la MISMA reserva #5 en SU pantalla
    //   3. Admin A cambia el título a "Reunión urgente" y guarda → OK
    //   4. Admin B cambia la descripción y guarda → OK
    //      PERO el UPDATE de B sobreescribe el título que A acaba de cambiar
    //      porque B tenía la versión antigua. A pierde su cambio silenciosamente.
    //
    // Situación con @Version:
    //   1. Admin A lee la reserva #5 → version = 0
    //   2. Admin B lee la reserva #5 → version = 0
    //   3. Admin A guarda → Hibernate genera:
    //      UPDATE reservations SET title='Reunión urgente', version=1
    //      WHERE id=5 AND version=0
    //      → version era 0, el WHERE coincide, UPDATE exitoso, version pasa a 1
    //
    //   4. Admin B guarda → Hibernate genera:
    //      UPDATE reservations SET description='Nueva desc', version=1
    //      WHERE id=5 AND version=0
    //      → version ya es 1 (la cambió A), el WHERE NO coincide
    //      → 0 filas actualizadas → Hibernate lanza OptimisticLockException
    //      → Nuestro GlobalExceptionHandler devuelve HTTP 409:
    //        "El recurso fue modificado por otro usuario"
    //
    // Se llama "optimista" porque ASUME que los conflictos son raros
    // y no bloquea la fila al leerla. Solo verifica en el momento de guardar.
    //
    // La alternativa (pesimista) bloquea la fila al LEERLA,
    // impidiendo que nadie más la lea hasta que termines.
    // Es más seguro pero destroza el rendimiento.
    //
    // REGLA IMPORTANTE: tú NUNCA modificas este campo manualmente.
    // JPA lo gestiona automáticamente. Si haces reservation.setVersion(5),
    // estás rompiendo el mecanismo de bloqueo y causarás bugs.
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

}
