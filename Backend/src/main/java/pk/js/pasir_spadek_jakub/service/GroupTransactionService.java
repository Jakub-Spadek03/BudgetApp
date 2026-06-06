package pk.js.pasir_spadek_jakub.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import pk.js.pasir_spadek_jakub.dto.GroupTransactionDTO;
import pk.js.pasir_spadek_jakub.model.Debt;
import pk.js.pasir_spadek_jakub.model.Group;
import pk.js.pasir_spadek_jakub.model.Membership;
import pk.js.pasir_spadek_jakub.model.User;
import pk.js.pasir_spadek_jakub.repository.DebtRepository;
import pk.js.pasir_spadek_jakub.repository.GroupRepository;
import pk.js.pasir_spadek_jakub.repository.MembershipRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GroupTransactionService {

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final MembershipService membershipService;
    private final GroupNotificationService groupNotificationService;

    public GroupTransactionService(GroupRepository groupRepository,
                                   MembershipRepository membershipRepository,
                                   DebtRepository debtRepository,
                                   MembershipService membershipService,
                                   GroupNotificationService groupNotificationService) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.debtRepository = debtRepository;
        this.membershipService = membershipService;
        this.groupNotificationService = groupNotificationService;
    }

    public void addGroupTransaction(GroupTransactionDTO transactionDTO, User currentUser) {
        Group group = groupRepository.findById(transactionDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono Grupy"));

        membershipService.assertCurrentUserIsGroupMember(group.getId());

        List<Membership> members = membershipRepository.findByGroupId(group.getId());
        List<Membership> selectedMembers = selectParticipants(transactionDTO, members, currentUser);

        if (selectedMembers.isEmpty()) {
            throw new IllegalStateException("Grupa nie ma czlonkow, nie mozna dodac transakcji.");
        }

        double amountPerUser = transactionDTO.getAmount() / selectedMembers.size();
        boolean expense = "EXPENSE".equals(transactionDTO.getType());

        for (Membership member : selectedMembers) {
            User otherUser = member.getUser();
            if (!otherUser.getId().equals(currentUser.getId())) {
                Debt debt = new Debt();
                debt.setDebtor(expense ? otherUser : currentUser);
                debt.setCreditor(expense ? currentUser : otherUser);
                debt.setGroup(group);
                debt.setAmount(amountPerUser);
                debt.setTitle(transactionDTO.getTitle());
                debtRepository.save(debt);
            }
        }

        groupNotificationService.notifyGroupMembers(
                group,
                selectedMembers,
                currentUser,
                transactionDTO.getTitle(),
                transactionDTO.getAmount(),
                amountPerUser
        );
    }

    private List<Membership> selectParticipants(
            GroupTransactionDTO transactionDTO,
            List<Membership> members,
            User currentUser) {

        List<Long> selectedUserIds = transactionDTO.getSelectedUserIds();
        if (selectedUserIds == null || selectedUserIds.isEmpty()) {
            return members;
        }

        Set<Long> uniqueSelectedUserIds = new HashSet<>(selectedUserIds);
        List<Membership> selectedMembers = members.stream()
                .filter(membership -> uniqueSelectedUserIds.contains(membership.getUser().getId()))
                .toList();

        if (selectedMembers.size() != uniqueSelectedUserIds.size()) {
            throw new IllegalStateException("Wszyscy wybrani uzytkownicy musza byc czlonkami grupy.");
        }

        boolean currentUserSelected = selectedMembers.stream()
                .anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));
        if (!currentUserSelected) {
            throw new IllegalStateException("Aktualny uzytkownik musi byc uczestnikiem transakcji grupowej.");
        }

        if (selectedMembers.size() < 2) {
            throw new IllegalStateException("Transakcja grupowa wymaga co najmniej dwoch uczestnikow.");
        }

        return selectedMembers;
    }
}