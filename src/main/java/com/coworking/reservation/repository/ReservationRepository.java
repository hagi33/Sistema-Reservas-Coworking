package com.coworking.reservation.repository;

import com.coworking.reservation.model.entity.Reservation;
import com.coworking.reservation.model.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/*
* @Repository marca esta interfaz como un componente de acceso a datos de Spring.
*  Spring la detecta al arrancar, la registra en su contenedor de beans, y le añade traducción de excepciones:
*  si PostgreSQL lanza una SQLException críptica, Spring la convierte en una DataAccessException más legible.
* */
@Repository
/*
*Para el repository usamos interfaces en vez de clase porque nosostros definimos los métodos que usaremos.
* */
/*
* extends JpaRepository<Reservation, Long> le dice a Spring Data dos cosas.
* El primer genérico (Reservation) es la entidad que gestiona este repositorio.
*  El segundo (Long) es el tipo del campo @Id de esa entidad.
* Con solo esta línea, heredas gratis más de 20 métodos: save(), findById(), findAll(), deleteById(), count(), existsById(), paginación, ordenamiento, etc.*/
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /*El método con @Query: la consulta de solapamiento
Este método es diferente porque la lógica es demasiado compleja para expresarla solo con el nombre. Un nombre como findByRoomIdAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan sería ilegible. Para estos casos, Spring Data permite escribir la query manualmente con @Query.
java@Query("""
    SELECT r FROM Reservation r
    WHERE r.room.id = :roomId
      AND r.status != :excludedStatus
      AND r.startTime < :endTime
      AND r.endTime > :startTime
""")
@Query le dice a Spring Data: "no intentes generar la query desde el nombre del método, usa esta que te doy yo". Las tres comillas """ son un text block de Java (desde Java 15), que permite escribir texto multilínea sin concatenar strings. Es solo azúcar sintáctico, el resultado es un string normal.
La query está escrita en JPQL, no en SQL. JPQL se parece mucho a SQL pero tiene diferencias importantes.
SELECT r FROM Reservation r en SQL sería SELECT * FROM reservations r. En JPQL usas el nombre de la clase Java (Reservation con mayúscula), no el nombre de la tabla SQL (reservations). La r es un alias: un nombre corto para referirte a la entidad en el resto de la query.
WHERE r.room.id = :roomId en SQL sería WHERE r.room_id = :roomId. En JPQL puedes navegar por las relaciones con el punto. r.room accede al campo room de Reservation (que es un objeto Room), y .id accede al id de ese Room. JPQL traduce esto internamente a la foreign key room_id.
:roomId es un parámetro con nombre. Los dos puntos indican que es un placeholder que se sustituirá por el valor real cuando se ejecute la query. Esto es una prepared statement: el valor nunca se concatena directamente en la query, lo que previene ataques de SQL injection.
AND r.status != :excludedStatus filtra por estado. Normalmente le pasaremos ReservationStatus.CANCELLED para excluir las reservas canceladas, porque una reserva cancelada no debería bloquear la disponibilidad de la sala.
Ahora viene la lógica de solapamiento, que es la parte que cuesta entender:
AND r.startTime < :endTime
AND r.endTime > :startTime
Para entender esto, piensa en dos reservas como barras en una línea temporal:
Reserva existente (r):     |----10:00---------12:00----|
Nueva reserva:                      |----11:00---------13:00----|
¿Cuándo se solapan dos rangos? Cuando uno empieza ANTES de que el otro termine, Y termina DESPUÉS de que el otro empiece.
La primera condición r.startTime < :endTime dice: "la reserva existente empieza antes de que la nueva termine". En el ejemplo: 10:00 < 13:00, sí se cumple.
La segunda condición r.endTime > :startTime dice: "la reserva existente termina después de que la nueva empiece". En el ejemplo: 12:00 > 11:00, sí se cumple.
Ambas se cumplen, por lo tanto HAY SOLAPAMIENTO.
Veamos un caso donde NO hay solapamiento:
Reserva existente (r):     |----10:00----12:00----|
Nueva reserva:                                         |----14:00----16:00----|
Primera condición: r.startTime < :endTime → 10:00 < 16:00, sí se cumple.
Segunda condición: r.endTime > :startTime → 12:00 > 14:00, NO se cumple.
No se cumplen las dos a la vez, por lo tanto NO hay solapamiento. La reserva existente no aparece en los resultados.
Otro caso sin solapamiento:
Nueva reserva:              |----08:00----09:00----|
Reserva existente (r):                                |----10:00----12:00----|
Primera condición: r.startTime < :endTime → 10:00 < 09:00, NO se cumple.
Ya falla la primera, por lo tanto NO hay solapamiento.
 * */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.room.id = :roomId
        AND r.status != :excludedStatus
        AND r.startTime < :endTime
        AND r.endTime > :startTime
    """)
    List<Reservation> findOverlappingReservations(
            /*
            * @Param("roomId") vincula el parámetro Java roomId con el placeholder :roomId de la query JPQL. Cuando llamas al metodo
            * */
            @Param("roomId") Long roomId,
            @Param("startTime")LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludedStatus")ReservationStatus excludedStatus

            );


    /*
    * El metodo lanza una query de SQL que es la siguienet : SELECT * FROM reservations WHERE user_id = ?
    * Devuelve una lista de reservas porque el usuario pude tener varias reservas, One To Many*/
    List<Reservation> findByUserId(Long Id);

    /*
    * Aquí hay dos condiciones encadenadas:
    RoomId → filtra por room.id
    And → operador AND
    StatusNot → el campo status debe ser DISTINTO al valor que pases
    * SELECT * FROM reservations WHERE room_id = ? AND status != ?
    * Se usa para mostrar las reservas activas de una sala excluyendo las canceladas:
    * */
    List<Reservation> findByRoomIdAndStatusNot(Long roomId,ReservationStatus status);


    /*
    *Dos condiciones con AND, pero esta vez sin Not: SELECT * FROM reservations WHERE user_id = ? AND status = ?
    **/
    List<Reservation> findByUserIdAndStatus(Long userId, ReservationStatus status);

    /*
    *count → en vez de SELECT , hace SELECT COUNT()
    ByUserId → filtra por usuario
    And → operador AND
    StatusIn → el campo status debe estar DENTRO de una lista de valores
    Query generada: SELECT COUNT(*) FROM reservations WHERE user_id = ? AND status IN (?, ?)
    *Se usa para limitar cuántas reservas activas puede tener un usuario:
    * */
    long countByUserIdAndStatusIn(Long userId, List<Reservation> statuses);

}
