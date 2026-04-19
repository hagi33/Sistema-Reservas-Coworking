package com.coworking.reservation.model.enums;


//Los valores del ENUM deben coincidir con los puestos
//en el check constraint de los ficheros SQL creados en el paquete db.migration
//De lo contrario Hibernate guardaría un valor diferente en la BD y el check lo rechazaría
public enum Role {

    USER,
    ADMIN
}
