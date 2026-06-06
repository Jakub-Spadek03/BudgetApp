package pk.js.pasir_spadek_jakub.service;

import org.springframework.stereotype.Service;
import pk.js.pasir_spadek_jakub.config.GroupWebSocketHandler;
import pk.js.pasir_spadek_jakub.dto.GroupNotificationDTO;
import pk.js.pasir_spadek_jakub.model.Group;
import pk.js.pasir_spadek_jakub.model.Membership;
import pk.js.pasir_spadek_jakub.model.User;

import java.util.List;

@Service
public class GroupNotificationService {

    private final GroupWebSocketHandler groupWebSocketHandler;

    public GroupNotificationService(GroupWebSocketHandler groupWebSocketHandler) {
        this.groupWebSocketHandler = groupWebSocketHandler;
    }

    public void notifyGroupMembers(Group group, List<Membership> participants,
                                   User createdBy, String title,
                                   Double amount, Double userShare) {

        System.out.println("=== WYSYLAM POWIADOMIENIE do grupy: " + group.getId());

        for (Membership membership : participants) {
            User member = membership.getUser();
            if (member.getId().equals(createdBy.getId())) {
                continue;
            }

            String message = String.format(
                    "%s dodał wydatek \"%s\" w grupie %s. Twoja część: %.2f zł.",
                    createdBy.getEmail(), title, group.getName(), userShare
            );

            GroupNotificationDTO notification = new GroupNotificationDTO(
                    "GROUP_EXPENSE_ADDED",
                    group.getId(),
                    group.getName(),
                    title,
                    amount,
                    userShare,
                    createdBy.getEmail(),
                    message
            );

            groupWebSocketHandler.sendToUser(member.getEmail(), notification);
        }
    }
}