package pk.js.pasir_spadek_jakub.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class TransactionDTO {

    @NotNull(message = "Kwota nie może być pusta.")
    @DecimalMin(value = "0.01", message = "Kwota musi być większa od zera.")
    private Double amount;

    @NotNull(message = "Typ transakcji jest wymagany.")
    @Pattern(regexp = "INCOME|EXPENSE", message = "Typ transakcji musi być INCOME lub EXPENSE.")
    private String type;

    @Size(max = 50, message = "Tagi nie mogą przekraczać 50 znaków.")
    private String tags;

    @Size(max = 255, message = "Notatka może mieć maksymalnie 255 znaków.")
    private String notes;

}
