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


    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    @Builder.Default
    private Integer floor = 1;


    @Column(name = "has_projector", nullable = false)
    @Builder.Default
    private Boolean hasProjector = false;

    @Column(name = "has_whiteboard", nullable = false)
    @Builder.Default
    private Boolean hasWhiteboard = false;

    @Column(name = "has_video_call", nullable = false)
    @Builder.Default
    private Boolean hasVideoXCall = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal pricePerHour = BigDecimal.ZERO;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();





}
