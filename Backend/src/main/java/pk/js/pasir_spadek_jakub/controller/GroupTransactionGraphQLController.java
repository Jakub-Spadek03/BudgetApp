package pk.js.pasir_spadek_jakub.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import pk.js.pasir_spadek_jakub.dto.GroupTransactionDTO;
import pk.js.pasir_spadek_jakub.model.User;
import pk.js.pasir_spadek_jakub.service.CurrentUserService;
import pk.js.pasir_spadek_jakub.service.GroupTransactionService;

@Controller
public class GroupTransactionGraphQLController {

    private final GroupTransactionService groupTransactionService;
    private final CurrentUserService currentUserService;

    public GroupTransactionGraphQLController(GroupTransactionService groupTransactionService,
                                             CurrentUserService currentUserService) {
        this.groupTransactionService = groupTransactionService;
        this.currentUserService = currentUserService;
    }

    @MutationMapping
    public Boolean addGroupTransaction(@Valid @Argument GroupTransactionDTO groupTransactionDTO) {
        User user = currentUserService.getCurrentUser();
        groupTransactionService.addGroupTransaction(groupTransactionDTO, user);
        return true;
    }
}