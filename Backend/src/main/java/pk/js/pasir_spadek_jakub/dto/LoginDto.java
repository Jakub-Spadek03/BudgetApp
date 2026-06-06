package pk.js.pasir_spadek_jakub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDto {

    @NotBlank(message = "Adres e-mail jest wymagany")
    @Email(message = "Podaj poprawny adres e-mail")
    private String email;

    @NotBlank(message = "Hasło nie może być puste")
    private String password;
}
