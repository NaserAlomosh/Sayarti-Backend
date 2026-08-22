package com.sayarti.backend.reference.repository;
import com.sayarti.backend.reference.entity.Country;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CountryRepository extends JpaRepository<Country, String> {
    Optional<Country> findByCodeAndActiveTrue(String code);
    List<Country> findAllByActiveTrueOrderByCodeAsc();
}
