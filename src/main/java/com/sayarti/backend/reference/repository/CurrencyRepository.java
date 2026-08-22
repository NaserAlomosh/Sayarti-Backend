package com.sayarti.backend.reference.repository;
import com.sayarti.backend.reference.entity.Currency;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CurrencyRepository extends JpaRepository<Currency, String> {
    Optional<Currency> findByCodeAndActiveTrue(String code);
    List<Currency> findAllByActiveTrueOrderByCodeAsc();
}
