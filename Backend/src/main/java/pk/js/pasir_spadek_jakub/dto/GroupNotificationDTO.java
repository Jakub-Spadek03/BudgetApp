package pk.js.pasir_spadek_jakub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GroupNotificationDTO {
    private String type;
    private Long groupId;
    private String groupName;
    private String title;
    private Double amount;
    private Double userShare;
    private String createdByEmail;
    private String message;
}