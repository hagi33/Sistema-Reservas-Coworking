package com.coworking.reservation.repository;

import com.coworking.reservation.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/*Lo que está pasando aquí es que JpaRepository<User, Long> te regala gratis todos los métodos CRUD básicos: save(), findById(), findAll(), deleteById(), count() y muchos más.
 Los genéricos le dicen a Spring Data que este repositorio trabaja con la entidad User y que su clave primaria es de tipo Long*/
@Repository
public interface UserRepository extends JpaRepository<User, Long> {


    /*
    * Los dos métodos que definimos son custom. Spring Data los implementa automáticamente analizando el nombre del método. findByEmail le dice a Spring:
    *  "genera un SELECT * FROM users WHERE email = ?".
    *  existsByEmail genera un SELECT EXISTS(SELECT 1 FROM users WHERE email = ?).
    *  No escribes SQL, no escribes implementación, solo el nombre del método.
    * */
    Optional<User> findByEmail(String email);

    boolean existByEmail(String email);

}
