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

    /*
    * @Id le dice a JPA: "este campo es la clave primaria de la tabla".
    *  Cada entidad JPA debe tener exactamente un campo marcado con @Id.
    *  JPA lo usa para identificar cada registro de forma única.
    * */
    @Id
    /*
    * @GeneratedValue(strategy = GenerationType.IDENTITY) le dice a JPA: "no me des tú el valor del id, déjalo en manos de la base de datos".
    *  IDENTITY significa que PostgreSQL usa su mecanismo de autoincremento (BIGSERIAL en la migración SQL).
    *  Cuando guardas un usuario nuevo sin poner id, PostgreSQL asigna el siguiente número disponible (1, 2, 3...) y
    *  JPA lo lee de vuelta para ponerlo en el objeto Java.
    * */
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
    *@Column personaliza como se mapea este campo Java a una columna de la tabla SQL.
    * name = created_at establece el nombre de la columna en la base de datos.
    * nullable = false, le dice a Hibernate que valide que el campo no sea null.
    * updatable = false, le dice a Hibernate que nunca incluya este campo en una sentencia UPDATE.
    * */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /*
    *Igual que el método anterior, solamente que este si que se incluye cuando se hace
    *una sentencia UPDATE en sql
    * */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /*
    * @Prepersist es un callback del ciclo de vida de JPA, le dice a Hibernate
    * "ejecuta este método justo antes de hacer el insert".
    * Tú nunca llamas a onCreate() directamente. Cuando haces userRepository.save(nuevoUsuario),
    *  internamente ocurre esto:
    *1. Hibernate detecta que es un objeto nuevo (su id es null)
    *2. Hibernate llama a onCreate() automáticamente
    *3. onCreate() pone la fecha actual en createdAt y updatedAt
    *4. Hibernate genera y ejecuta el INSERT con esos valores
    *protected significa que el método es accesible desde esta clase y
    *desde las clases hijas (User, Room, Reservation), pero no desde fuera.
    *No queremos que nadie llame a user.onCreate() directamente desde un servicio.
    * LocalDateTime.now() devuelve la fecha y hora actual del servidor en el momento de la ejecución.
    *  Ambos campos reciben la misma fecha porque al crear un registro,
    *  la fecha de creación y la de última modificación son la misma.
    * prepersist = "antes de persistencia"
    * */
    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

    }

    /*
    * @PreUpdate funciona igual que @PrePersist pero se ejecuta antes de un UPDATE en vez de un INSERT.
    * Cada vez que modificas cualquier campo de una entidad y haces save(),
    *  Hibernate llama a onUpdate() automáticamente y updatedAt se actualiza a la fecha actual.
    *Solo actualiza updatedAt, no createdAt, porque la fecha de creación nunca cambia.
    *  Además, aunque alguien intentara cambiar createdAt aquí,
    *  el updatable = false de su @Column lo bloquearía.
    * */
    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

}
