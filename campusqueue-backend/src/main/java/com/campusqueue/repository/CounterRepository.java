package com.campusqueue.repository;

import com.campusqueue.entity.Counter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounterRepository extends JpaRepository<Counter, Long> {

    List<Counter> findByIsActiveTrue();

    Optional<Counter> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByName(String name);
}
