package pk.js.pasir_spadek_jakub.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import pk.js.pasir_spadek_jakub.dto.GroupDTO;
import pk.js.pasir_spadek_jakub.model.Group;
import pk.js.pasir_spadek_jakub.model.Membership;
import pk.js.pasir_spadek_jakub.model.User;
import pk.js.pasir_spadek_jakub.repository.DebtRepository;
import pk.js.pasir_spadek_jakub.repository.GroupRepository;
import pk.js.pasir_spadek_jakub.repository.MembershipRepository;

import java.util.List;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final CurrentUserService currentUserService;

    public GroupService(GroupRepository groupRepository,
                        MembershipRepository membershipRepository,
                        DebtRepository debtRepository,
                        CurrentUserService currentUserService) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.debtRepository = debtRepository;
        this.currentUserService = currentUserService;
    }

    public List<Group> getAllGroups() {
        User currentUser = currentUserService.getCurrentUser();
        return groupRepository.findByMemberships_User(currentUser);
    }

    public Group createGroup(GroupDTO groupDTO) {
        User owner = currentUserService.getCurrentUser();
        Group group = new Group();
        group.setName(groupDTO.getName());
        group.setOwner(owner);
        Group savedGroup = groupRepository.save(group);
        Membership membership = new Membership();
        membership.setUser(owner);
        membership.setGroup(savedGroup);
        membershipRepository.save(membership);
        return savedGroup;
    }

    @Transactional
    public void deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można usunąć grupy. Grupa o ID " + id + " nie istnieje."));

        User currentUser = currentUserService.getCurrentUser();
        if (!group.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Tylko właściciel grupy może ją usunąć.");
        }

        debtRepository.deleteByGroupId(id);
        membershipRepository.deleteByGroupId(id);
        groupRepository.delete(group);
    }
}