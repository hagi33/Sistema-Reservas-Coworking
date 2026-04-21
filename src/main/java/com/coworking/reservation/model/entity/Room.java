package com.coworking.reservation.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room extends BaseEntity{


    // --- CAMPO: name ---
    // Nombre de la sala. Debe ser único en todo el sistema.
    // Ejemplos: "Sala Goya", "Sala Innovation Lab", "Sala 3B"
    //
    // unique = true → le dice a Hibernate que hay un índice UNIQUE
    // en esta columna. Flyway lo creó en la migración.
    // Si intentas crear dos salas con el mismo nombre,
    // PostgreSQL rechaza el INSERT por violación del UNIQUE.
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // --- CAMPO: description ---
    //
    // columnDefinition = "TEXT" → le dice a Hibernate que use el tipo TEXT
    // de PostgreSQL en vez de VARCHAR para esta columna.
    // TEXT no tiene límite de longitud (VARCHAR sí, necesita un número).
    // Útil para descripciones que pueden ser desde una frase hasta varios párrafos.
    //
    // NO tiene nullable = false → este campo es OPCIONAL.
    // Puedes crear una sala sin descripción (el campo será null en la BD).
    // En el JSON de respuesta, como tenemos configurado
    // default-property-inclusion: non_null en application.yml,
    // si description es null, simplemente no aparece en el JSON.
    @Column(columnDefinition = "TEXT")
    private String description;

    // --- CAMPO: capacity ---
    // Número máximo de personas que caben en la sala.
    // Se usa para filtrar: "necesito una sala para 10 personas".
    // El CHECK constraint en la BD (chk_room_capacity) asegura que sea > 0.
    // Integer (wrapper) en vez de int (primitivo) por la misma razón
    // que Boolean en User: Hibernate necesita poder representar null temporalmente.
    @Column(nullable = false)
    private Integer capacity;

    // --- CAMPO: floor ---
    // Planta del edificio donde está la sala.
    // @Builder.Default con valor 1 → si no especificas planta al crear la sala,
    // se asume planta 1 (planta baja o primera, según el edificio).
    @Column(nullable = false)
    @Builder.Default
    private Integer floor = 1;

    // --- CAMPOS DE EQUIPAMIENTO ---
    //
    // Tres booleanos que indican qué equipamiento tiene la sala.
    //
    // ¿Por qué booleanos individuales y no una tabla separada "room_equipment"
    // con relación @ManyToMany?
    //
    // Porque el equipamiento es un conjunto FIJO y PEQUEÑO (3 opciones).
    // Los tipos no cambian frecuentemente. Con booleanos:
    //   - Una sola query carga la sala CON su equipamiento
    //   - No necesitas JOIN con otra tabla
    //   - El código es más simple
    //
    // Si tuviéramos 20 tipos de equipamiento o si cambiaran frecuentemente,
    // SÍ usaríamos una tabla separada con @ManyToMany.
    //
    // name = "has_projector" → mapeo explícito porque el campo Java
    // "hasProjector" (camelCase) se traduce a "has_projector" (snake_case).
    // Spring Boot lo haría automáticamente, pero lo ponemos explícito.
    //
    // @Builder.Default con false → por defecto la sala NO tiene
    // ningún equipamiento especial. Lo activas explícitamente:
    //   Room.builder().name("Sala Pro").hasProjector(true).build()
    @Column(name = "has_projector", nullable = false)
    @Builder.Default
    private Boolean hasProjector = false;

    @Column(name = "has_whiteboard", nullable = false)
    @Builder.Default
    private Boolean hasWhiteboard = false;

    @Column(name = "has_video_call", nullable = false)
    @Builder.Default
    private Boolean hasVideoCall = false;

    // --- CAMPO: active ---
    // Soft delete, igual que en User.
    // Una sala desactivada no aparece en las búsquedas de disponibilidad
    // pero mantiene su historial de reservas pasadas
    // (necesario para auditoría y facturación).
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // --- CAMPO: pricePerHour ---
    //
    // BigDecimal en vez de double/float. Ver el import de arriba para la explicación.
    //
    // precision = 10: total de dígitos que puede tener el número.
    // scale = 2: de esos 10 dígitos, 2 van después del punto decimal.
    // → Rango: desde 0.00 hasta 99999999.99
    // Debe coincidir con DECIMAL(10, 2) de la migración SQL.
    //
    // BigDecimal.ZERO es la constante 0.00 de BigDecimal.
    // Es preferible a new BigDecimal("0.00") porque reutiliza el mismo objeto.
    //
    // El CHECK constraint en la BD (chk_room_price) asegura que sea >= 0.
    // No puede haber precios negativos.
    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal pricePerHour = BigDecimal.ZERO;

    // --- CAMPO: reservations (RELACIÓN) ---
    // Exactamente la misma lógica que User.reservations.
    //
    // mappedBy = "room" → apunta al campo "private Room room"
    // de la clase Reservation (el lado que tiene la foreign key room_id).
    //
    // Una sala tiene muchas reservas. Cada reserva pertenece a una sola sala.
    // Revisa User.java para la explicación detallada de @OneToMany,
    // cascade, fetch y @Builder.Default.
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();




}
