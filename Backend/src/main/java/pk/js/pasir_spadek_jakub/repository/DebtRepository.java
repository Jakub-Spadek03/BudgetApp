package pk.js.pasir_spadek_jakub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pk.js.pasir_spadek_jakub.model.Debt;

import java.util.List;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {
    List<Debt> findByGroupId(Long groupId);
    void deleteByGroupId(Long groupId);
}