package pk.js.pasir_spadek_jakub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pk.js.pasir_spadek_jakub.model.Group;
import pk.js.pasir_spadek_jakub.model.User;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByMemberships_User(User user);
}