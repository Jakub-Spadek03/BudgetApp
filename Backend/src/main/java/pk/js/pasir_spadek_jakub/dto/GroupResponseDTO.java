package pk.js.pasir_spadek_jakub.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GroupResponseDTO {

    private Long id;
    private String name;
    private Long ownerId;
}
