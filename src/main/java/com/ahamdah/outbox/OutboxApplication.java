package com.ahamdah.outbox;

import jakarta.persistence.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootApplication
@RestController
@RequiredArgsConstructor
@Slf4j
@EnableJpaAuditing
public class OutboxApplication implements CommandLineRunner {

    private final TransactionRepository transactionRepository;
    private final TransactionOutboxRepository transactionOutboxRepository;

    public static void main(String[] args) {
        SpringApplication.run(OutboxApplication.class, args);
    }

    @GetMapping("/")
    public String hello(){
        return "Hello World";
    }

    @GetMapping("/create")
    @Transactional
    public Transaction hello(HttpServletResponse response){

        //===========================
        log.info("creating Transaction");
        var transaction=transactionRepository.save(Transaction.builder().amount(BigDecimal.valueOf(Math.random()*100)).build());

        //===========================
        log.info("creating Transaction Event");
        var event=transactionOutboxRepository.save(
                TransactionOutboxEvent.builder()
                        .payload(transaction.toString())
                        .eventType("pending")
                        .aggregateId(transaction.getId().toString())
                .build());
        log.info("Event created{}", event);
        return transaction;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Application started");
    }
}

@Entity
@Builder@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    private BigDecimal amount;

    @CreatedDate
    LocalDate createdDate;

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", amount=" + amount +
                ", createdDate=" + createdDate +
                '}';
    }
}

@Entity
@Builder@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
class TransactionOutboxEvent{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "aggregate_id")
    private String aggregateId;

    private String payload;

    @Column(name = "event_type")
    private String eventType;

    @CreatedDate
    LocalDate createdDate;

}

interface TransactionRepository extends JpaRepository<Transaction, Long> {}
interface TransactionOutboxRepository extends JpaRepository<TransactionOutboxEvent, Long> {}