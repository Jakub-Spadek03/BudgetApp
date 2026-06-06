package pk.js.pasir_spadek_jakub.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pk.js.pasir_spadek_jakub.dto.BalanceDto;
import pk.js.pasir_spadek_jakub.dto.TransactionDTO;
import pk.js.pasir_spadek_jakub.model.Transaction;
import pk.js.pasir_spadek_jakub.model.TransactionType;
import pk.js.pasir_spadek_jakub.model.User;
import pk.js.pasir_spadek_jakub.repository.TransactionRepository;
import pk.js.pasir_spadek_jakub.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public List<Transaction> getAllTransactions() {
        User user = getCurrentUser();
        return transactionRepository.findAllByUser(user);
    }

//    public Transaction getTransactionById(Long id) {
//        return transactionRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID " + id));
//
//        if (!transaction.getUser().getEmail().equals(getCurrentUser().getEmail())) {
//            throw new AccessDeniedException("Nie masz dostępu do tej transakcji");
//        }
//    }
    public Transaction getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID " + id));

        if (!transaction.getUser().getEmail().equals(getCurrentUser().getEmail())) {
            throw new AccessDeniedException("Nie masz dostępu do tej transakcji");
        }

        return transaction;
    }
    public Transaction updateTransaction(Long id, TransactionDTO transactionDTO) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID " + id));

        if (!transaction.getUser().getEmail().equals(getCurrentUser().getEmail())) {
            throw new AccessDeniedException("Nie masz dostępu do tej transakcji");
        }
        transaction.setAmount(transactionDTO.getAmount());
        transaction.setType(TransactionType.valueOf(transactionDTO.getType()));
        transaction.setTags(transactionDTO.getTags());
        transaction.setNotes(transactionDTO.getNotes());


        return transactionRepository.save(transaction);
    }

    public Transaction createTransaction(TransactionDTO transactionDTO) {
        Transaction transaction = new Transaction();

        transaction.setAmount(transactionDTO.getAmount());
        transaction.setType(TransactionType.valueOf(transactionDTO.getType()));
        transaction.setTags(transactionDTO.getTags());
        transaction.setNotes(transactionDTO.getNotes());
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setUser(getCurrentUser());

        return transactionRepository.save(transaction);
    }

//    public void deleteTransaction(Long id) {
//        if (!transactionRepository.existsById(id)) {
//            throw new EntityNotFoundException("Nie znaleziono transakcji o ID " + id);
//        }
//        transactionRepository.deleteById(id);
//    }

    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID " + id));

        if (!transaction.getUser().getEmail().equals(getCurrentUser().getEmail())) {
            throw new AccessDeniedException("Nie masz dostępu do tej transakcji");
        }

        transactionRepository.delete(transaction);
    }

    public BalanceDto getUserBalance(User user, Float days) {
        List<Transaction> userTransactions;

        if (days != null) {
            LocalDateTime from = LocalDateTime.now().minusDays(days.longValue());
            userTransactions = transactionRepository.findAllByUserAndTimestampGreaterThanEqual(user, from);
        } else {
            userTransactions = transactionRepository.findByUser(user);
        }

        double income = userTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double expense = userTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();

        return new BalanceDto(income, expense, income - expense);
    }


    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("użytkownik nie jest uwierzytelniony");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono zalogowanego użytkownika: " + email));
    }


}
