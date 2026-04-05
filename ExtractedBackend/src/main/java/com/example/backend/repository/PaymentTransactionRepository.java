package com.example.backend.repository;

import com.example.backend.model.PaymentTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends MongoRepository<PaymentTransaction, String> {
    Optional<PaymentTransaction> findByOrderId(String orderId);
}

