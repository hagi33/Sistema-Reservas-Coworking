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
// --- CAMPO: name ---

    // @Column → configura el mapeo a la columna SQL.
    //
    // nullable = false:
    //   Hibernate valida que name no sea null antes del INSERT.
    //   Si intentas guardar un User con name = null, Hibernate lanza excepción
    //   ANTES de enviar nada a PostgreSQL.
    //   Complementa el NOT NULL de la migración SQL (doble protección).
    //
    // length = 100:
    //   Le dice a Hibernate que la columna es VARCHAR(100).
    //   Debe coincidir con el VARCHAR(100) de la migración V1.
    //   Si no coinciden, al arrancar con ddl-auto: validate,
    //   Hibernate detecta la discrepancia y la aplicación falla
    //   con un error que dice exactamente qué no coincide.
    //
    // No tiene name = "..." porque el campo Java (name) coincide
    // con la columna SQL (name). No necesita conversión.
    @Column(nullable = false, length = 100)
    private String name;

    // --- CAMPO: email ---

    // unique = true:
    //   Le dice a Hibernate que hay un índice UNIQUE en esta columna.
    //   Hibernate NO crea el índice (lo hizo Flyway en la migración),
    //   pero sabe que existe y lo usa para:
    //   - Optimizar queries internas
    //   - Dar mensajes de error más claros si hay duplicados
    //
    // El email es el identificador para el login. Cuando el usuario
    // envía sus credenciales, buscamos por email con findByEmail().
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // --- CAMPO: password ---

    // Sin "length" porque la migración usa VARCHAR(255)
    // y el valor por defecto de @Column sin length es exactamente 255.
    //
    // IMPORTANTE: aquí NUNCA se guarda la contraseña en texto plano.
    // Se guarda el HASH generado por BCrypt.
    //
    // Cuando el usuario se registra con "hola123":
    // 1. El servicio usa BCryptPasswordEncoder.encode("hola123")
    // 2. Obtiene algo como "$2a$10$N9qo8uLOickgx2ZMRZoMye..."
    // 3. Guarda ESE hash en la BD
    //
    // Cuando el usuario hace login:
    // 1. Envía "hola123" en la petición
    // 2. El servicio lee el hash de la BD
    // 3. Usa BCrypt.matches("hola123", hashGuardado)
    // 4. BCrypt compara internamente y devuelve true o false
    //
    // Ni siquiera nosotros podemos ver la contraseña real.
    // BCrypt es un hash de UNA DIRECCIÓN: no se puede revertir.
    @Column(nullable = false)
    private String password;

    // --- CAMPO: role ---

    // @Enumerated(EnumType.STRING) → JPA guarda el TEXTO del enum en la BD.
    // Role.ADMIN se guarda como la cadena "ADMIN" en la columna.
    // Cuando lee de la BD, convierte "ADMIN" de vuelta a Role.ADMIN.
    //
    // La alternativa EnumType.ORDINAL guardaría la POSICIÓN numérica:
    //   Role.USER = 0, Role.ADMIN = 1
    //
    // ORDINAL es peligroso porque si mañana añades Role.MANAGER entre
    // USER y ADMIN:
    //   Role.USER = 0, Role.MANAGER = 1 (nuevo), Role.ADMIN = 2 (antes era 1)
    //   Todos los admins en la BD tenían ordinal 1.
    //   Ahora 1 = MANAGER. Los admins pasan a ser managers silenciosamente.
    //   Corrupción de datos sin ningún error ni aviso.
    //   Con STRING eso NO pasa: "ADMIN" siempre es "ADMIN".
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)

    // @Builder.Default → cuando usas el Builder, si NO especificas el rol,
    // usa Role.USER como valor por defecto.
    //
    // TRAMPA DE LOMBOK: sin @Builder.Default, el valor por defecto del campo
    // (= Role.USER) se IGNORA cuando usas el Builder.
    //
    //   Sin @Builder.Default:
    //     User.builder().name("Ana").build()  →  role = null (¡MAL!)
    //
    //   Con @Builder.Default:
    //     User.builder().name("Ana").build()  →  role = Role.USER (correcto)
    //
    // Esto aplica a TODOS los campos que tengan @Builder.Default en esta clase.
    @Builder.Default
    private Role role = Role.USER;

    // --- CAMPO: active ---

    // Boolean con MAYÚSCULA (tipo wrapper) en vez de boolean con minúscula (tipo primitivo).
    //
    // Diferencia:
    //   Boolean → puede ser null, true o false
    //   boolean → solo puede ser true o false (nunca null)
    //
    // Usamos Boolean porque Hibernate trabaja mejor con tipos wrapper.
    // Internamente, Hibernate necesita representar "este campo aún no tiene valor"
    // durante el proceso de hidratación (rellenar el objeto con datos de la BD),
    // y null le sirve para indicar eso temporalmente.
    //
    // @Builder.Default con true → los usuarios nuevos están activos por defecto.
    //
    // SOFT DELETE: para "borrar" un usuario, ponemos active = false
    // en vez de eliminarlo de la BD. ¿Por qué?
    // Si borras un usuario con DELETE, tienes dos problemas:
    // 1. Las reservas de ese usuario tienen user_id apuntando a un registro que ya no existe
    //    → la foreign key falla (o se borran las reservas en cascada, perdiendo historial)
    // 2. Pierdes toda la información del usuario para auditoría y reportes
    //
    // Con soft delete: el usuario "no existe" para el sistema (no puede hacer login,
    // no aparece en listados) pero sus datos y reservas históricas siguen intactos.
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // --- CAMPO: reservations (RELACIÓN) ---

    // @OneToMany → declara la relación: UN usuario tiene MUCHAS reservas.
    //
    // mappedBy = "user":
    //   Le dice a JPA: "la foreign key de esta relación NO está en la tabla users.
    //   Está en la tabla reservations, en la columna que corresponde al campo
    //   llamado 'user' en la clase Reservation."
    //
    //   Si abres Reservation.java, verás:
    //     @ManyToOne
    //     @JoinColumn(name = "user_id")
    //     private User user;
    //
    //   ESE campo "user" es al que se refiere mappedBy.
    //   La columna user_id en la tabla reservations es la foreign key real.
    //
    //   Sin mappedBy, JPA asumiría que necesita una tabla intermedia
    //   "users_reservations" (como una relación muchos-a-muchos).
    //   Eso NO es lo que queremos.
    //
    // cascade = CascadeType.ALL:
    //   Las operaciones sobre el usuario se PROPAGAN a sus reservas.
    //   - Si guardas un usuario con reservas nuevas en su lista → se guardan las reservas
    //   - Si actualizas un usuario → se actualizan sus reservas modificadas
    //   - Si borras un usuario → se borrarían las reservas (pero usamos soft delete,
    //     así que nunca borramos usuarios directamente)
    //
    // fetch = FetchType.LAZY:
    //   Cuando cargas un usuario de la BD, las reservas NO se cargan.
    //   Hibernate crea un PROXY: un objeto que parece una lista pero está vacío.
    //   Solo cuando accedes a user.getReservations() (por ejemplo .size() o .get(0)),
    //   Hibernate ejecuta la query SELECT * FROM reservations WHERE user_id = ?
    //
    //   Si fuera EAGER, cada vez que cargas UN usuario, Hibernate carga
    //   TODAS sus reservas automáticamente. Si cargas 100 usuarios:
    //   - Con EAGER: 1 query para usuarios + 100 queries para reservas = 101 queries
    //   - Con LAZY: 1 query para usuarios + 0 queries extra = 1 query
    //   Las queries de reservas solo se ejecutan si accedes a ellas.
    //   Esto es el famoso problema N+1 que LAZY previene.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)

    // @Builder.Default con new ArrayList<>():
    // Sin esto, User.builder().name("Ana").build() tendría reservations = null.
    // Cualquier user.getReservations().add(reserva) lanzaría NullPointerException.
    // Con @Builder.Default, la lista empieza vacía pero EXISTE,
    // así que puedes añadir elementos sin problemas.
    @Builder.Default

    // List<Reservation> → lista de objetos Reservation.
    // Usamos List (la interfaz) en vez de ArrayList (la implementación)
    // como tipo del campo. Es buena práctica programar contra interfaces
    // porque Hibernate puede sustituir ArrayList por su propia implementación
    // interna (PersistentBag) y la interfaz lo permite.
    private List<Reservation> reservations = new ArrayList<>();




}
