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

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

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
    private final List<String> event_type= Arrays.asList("deposit","withdraw","transfer");

    @GetMapping("/create")
    @Transactional
    public Transaction createTransaction(){

        //===========================
        log.info("creating Transaction");

        Currency currency=Currency.getInstance("JOD");
        var transaction=transactionRepository.save(
                Transaction.
                        builder()
                        .amount(BigInteger.valueOf((int)(Math.random()*100)))
                        .currencyCode(currency.getCurrencyCode())
                        .build());

        //===========================
        log.info("creating Transaction Event");
        var event=transactionOutboxRepository.save(
                TransactionOutboxEvent.builder()
                        .payload(transaction.toString())
                        .eventType("deposit")
                        .aggregateId("1")
                        .status(EventStatus.PENDING)
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
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    private BigInteger amount;

    private String currencyCode;

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
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    //Use to Indicate the partition we can use the user id(All user Transaction will be in Ordered) or account id
    @Column(name = "aggregate_id")
    private String aggregateId;

    private String payload;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @CreatedDate
    LocalDate createdDate;

}

enum EventStatus{
    PENDING,PROCESSED,FAILED
}

interface TransactionRepository extends JpaRepository<Transaction, UUID> {}
interface TransactionOutboxRepository extends JpaRepository<TransactionOutboxEvent, UUID> {}