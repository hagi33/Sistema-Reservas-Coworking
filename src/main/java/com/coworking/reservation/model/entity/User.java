package com.coworking.reservation.model.entity;

import com.coworking.reservation.model.enums.Role;
import jakarta.persistence.*;
import jdk.jfr.Enabled;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/*
* Entity es la anotación que se encarga de convertir una clase Java
* en una entidad de la base de datos.
* */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity{

    @Column(nullable = false, length = 25)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean acttive = true;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();




}
