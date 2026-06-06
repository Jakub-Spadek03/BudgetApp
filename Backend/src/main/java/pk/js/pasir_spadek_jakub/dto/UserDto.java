package pk.js.pasir_spadek_jakub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {

    @NotBlank
    private String username;

    @NotBlank
    @Email(message = "Nieprawidłowy format adresu email")
    private String email;

    @NotBlank
    private String password;
}