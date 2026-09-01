package com.campusqueue.repository;

import com.campusqueue.entity.Counter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounterRepository extends JpaRepository<Counter, Long> {

    List<Counter> findByIsActiveTrue();

    Optional<Counter> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    /**
     * Pessimistic row-level lock on a Counter record during token generation.
     * Prevents race conditions and duplicate token numbers across concurrent requests.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Counter c WHERE c.id = :id")
    Optional<Counter> findByIdForUpdate(@Param("id") Long id);
}
