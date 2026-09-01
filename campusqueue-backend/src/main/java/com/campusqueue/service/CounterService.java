package com.campusqueue.service;

import com.campusqueue.dto.request.CreateCounterRequest;
import com.campusqueue.dto.response.CounterResponse;
import com.campusqueue.entity.Counter;
import com.campusqueue.exception.BadRequestException;
import com.campusqueue.exception.ResourceNotFoundException;
import com.campusqueue.repository.CounterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CounterService {

    private final CounterRepository counterRepository;

    public CounterService(CounterRepository counterRepository) {
        this.counterRepository = counterRepository;
    }

    @Transactional
    public CounterResponse createCounter(CreateCounterRequest request) {
        String code = request.getCode().trim().toUpperCase();
        String name = request.getName().trim();

        if (counterRepository.existsByName(name)) {
            throw new BadRequestException("Counter with name '" + name + "' already exists");
        }
        if (counterRepository.existsByCode(code)) {
            throw new BadRequestException("Counter with code '" + code + "' already exists");
        }

        Counter counter = new Counter(
                name,
                code,
                request.getDescription() != null ? request.getDescription().trim() : null,
                true
        );

        Counter savedCounter = counterRepository.save(counter);
        return CounterResponse.fromEntity(savedCounter);
    }

    @Transactional(readOnly = true)
    public CounterResponse getCounterById(Long id) {
        Counter counter = counterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found with id: " + id));
        return CounterResponse.fromEntity(counter);
    }

    @Transactional(readOnly = true)
    public List<CounterResponse> getAllCounters() {
        return counterRepository.findAll().stream()
                .map(CounterResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CounterResponse> getActiveCounters() {
        return counterRepository.findByIsActiveTrue().stream()
                .map(CounterResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CounterResponse toggleCounterStatus(Long id) {
        Counter counter = counterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found with id: " + id));
        counter.setIsActive(!counter.getIsActive());
        return CounterResponse.fromEntity(counterRepository.save(counter));
    }

    @Transactional(readOnly = true)
    public Counter findCounterEntity(Long id) {
        return counterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found with id: " + id));
    }
}
