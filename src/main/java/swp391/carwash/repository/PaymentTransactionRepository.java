package swp391.carwash.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.PaymentTransaction;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Integer> {
    boolean existsByProviderAndProviderTxnId(String provider, String providerTxnId);

    List<PaymentTransaction> findByPaymentIdOrderByCreatedAtDesc(Integer paymentId);
}
