package com.coworking.reservation.model.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
* Anotaciones de lombok para generar automaticamente los getters y setters
* */
@Getter
@Setter
/*
* La anotación @MappedSuperclass en JPA se utiliza para definir una clase base con atributos comunes (como id, fechaCreacion),
*  que son heredados por múltiples entidades. No representa una tabla en la base de datos por sí misma,
*  evitando la duplicación de código y facilitando el diseño DRY (Don't Repeat Yourself)
* Quiere decir que las clases que hereden de esta, tendran estos atributos en la BD también.
* */
@MappedSuperclass
public abstract class BaseEntity {
    // --- CAMPO: id ---

    // @Id → marca este campo como la PRIMARY KEY de la tabla.
    // Cada entidad JPA debe tener exactamente un campo con @Id.
    // JPA lo usa para identificar cada registro de forma única.
    // Cuando haces findById(5L), JPA busca el registro cuyo @Id vale 5.
    @Id

    // @GeneratedValue(strategy = GenerationType.IDENTITY) → le dice a JPA:
    // "No me des tú el valor del id, deja que la BD lo genere".
    //
    // IDENTITY = usa el mecanismo de autoincremento nativo de PostgreSQL.
    // En la migración SQL definimos la columna como BIGSERIAL,
    // que es un BIGINT con secuencia autoincremental.
    //
    // Cuando guardas un usuario nuevo sin poner id:
    // 1. JPA genera un INSERT sin el campo id
    // 2. PostgreSQL asigna el siguiente número (1, 2, 3...)
    // 3. JPA lee ese número generado y lo pone en el campo id del objeto Java
    //
    // Otras estrategias disponibles (no las usamos):
    // - SEQUENCE: usa una secuencia explícita de la BD (más eficiente en batch)
    // - TABLE: usa una tabla auxiliar para generar ids (portable pero lento)
    // - AUTO: JPA elige según la BD (impredecible, no recomendado)
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    // "private" → solo accesible desde dentro de esta clase.
    // Los getters/setters generados por Lombok actúan como intermediarios.
    //
    // "Long" (con mayúscula) → tipo wrapper que puede ser null.
    // A diferencia de "long" (minúscula) que no puede ser null.
    // Usamos Long porque antes de guardar en BD, el id es null
    // (todavía no se ha generado). JPA comprueba si id es null
    // para saber si debe hacer INSERT (nuevo) o UPDATE (existente).
    private Long id;

    // --- CAMPO: createdAt ---

    // @Column → personaliza cómo se mapea este campo a la columna SQL.
    //
    // name = "created_at":
    //   El campo Java se llama "createdAt" (camelCase, convención Java)
    //   pero la columna SQL se llama "created_at" (snake_case, convención SQL).
    //   Este atributo hace el puente entre ambas convenciones.
    //   Spring Boot haría esta conversión automáticamente,
    //   pero lo ponemos explícito para que el mapeo sea claro.
    //
    // nullable = false:
    //   Hibernate valida que este campo NO sea null ANTES de hacer el INSERT.
    //   Si intentas guardar una entidad con createdAt = null,
    //   Hibernate lanza una excepción antes de tocar la BD.
    //   Es una validación en la capa Java que complementa el NOT NULL del SQL.
    //
    // updatable = false:
    //   Hibernate NUNCA incluye este campo en sentencias UPDATE.
    //   La fecha de creación se establece UNA VEZ al crear el registro
    //   y no debe cambiar jamás.
    //   Si alguien hace entity.setCreatedAt(otraFecha) y luego save(),
    //   Hibernate ignora ese cambio: el UPDATE que genera simplemente
    //   no incluye la columna created_at.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // --- CAMPO: updatedAt ---

    // Igual que createdAt pero SIN updatable = false.
    // Este campo SÍ se actualiza en cada UPDATE porque su propósito
    // es registrar cuándo se modificó el registro por última vez.
    // El callback @PreUpdate (más abajo) se encarga de actualizarlo
    // automáticamente cada vez que se modifica la entidad.
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- CALLBACK: onCreate ---

    // @PrePersist → este método se ejecuta AUTOMÁTICAMENTE justo antes
    // de que Hibernate ejecute un INSERT en la base de datos.
    // Tú NUNCA llamas a onCreate() directamente. JPA lo llama por ti.
    //
    // Flujo cuando haces userRepository.save(nuevoUsuario):
    // 1. Hibernate detecta que id es null → es un INSERT (registro nuevo)
    // 2. Hibernate llama a onCreate() automáticamente
    // 3. onCreate() asigna la fecha actual a createdAt y updatedAt
    // 4. Hibernate genera el INSERT con esos valores
    // 5. PostgreSQL ejecuta el INSERT y genera el id
    //
    // "protected" → accesible desde esta clase y sus hijas (User, Room, Reservation)
    // pero no desde fuera. Nadie debería llamar a user.onCreate() desde un servicio.
    @PrePersist
    protected void onCreate() {
        // LocalDateTime.now() → fecha y hora actual del servidor.
        // Ambos campos reciben la misma fecha porque al crear un registro,
        // la fecha de creación y la de última modificación son la misma.
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // --- CALLBACK: onUpdate ---

    // @PreUpdate → se ejecuta AUTOMÁTICAMENTE antes de cada UPDATE.
    // Cada vez que modificas cualquier campo de cualquier entidad
    // y haces save(), Hibernate llama a onUpdate() antes de ejecutar el UPDATE.
    //
    // Solo actualiza updatedAt, NO createdAt, porque la fecha de creación
    // nunca cambia. Además, aunque alguien intentara cambiar createdAt aquí,
    // el updatable = false de su @Column lo bloquearía en el UPDATE.
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
