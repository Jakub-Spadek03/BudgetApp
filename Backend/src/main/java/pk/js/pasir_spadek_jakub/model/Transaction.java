package pk.js.pasir_spadek_jakub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The Transaction entity represents a single financial transaction.
 * Each transaction has a unique identifier, amount, type, tags, notes, and creation date.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transactions")   // You should use "transactions" instead of "transaction",

@SuppressWarnings("JpaDataSourceORMInspection") // Suppress IDE warning - table will be created by Hibernate// because "transaction" is a reserved word in SQL.
public class Transaction {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private String tags;

    private String notes;

    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Transaction(Double amount, TransactionType type, String tags, String notes, User user){
        this.amount = amount;
        this.type = type;
        this.tags = tags;
        this.notes = notes;
        this.user = user;
        this.timestamp = LocalDateTime.now();
    }

}
