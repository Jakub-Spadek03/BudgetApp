package pk.js.pasir_spadek_jakub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity // tells Spring that the class is an entity mapped to a table in the database
@Table(name = "users") // set the table name to avoid e.g. "user", which is a reserved word in some DBs
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nazwa użytkownika jest wymagana")
    private String username;

    @Email(message = "Podaj poprawny adres e-mail")
    @NotBlank(message = "Adres e-mail jest wymagany")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Hasło nie może byc puste")
    private String password;

    private String currency = "PLN"; // default currency - for initial account settings
}