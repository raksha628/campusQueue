package com.campusqueue.controller;

import com.campusqueue.dto.request.CreateCounterRequest;
import com.campusqueue.dto.response.CounterResponse;
import com.campusqueue.service.CounterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/counters")
public class CounterController {

    private final CounterService counterService;

    public CounterController(CounterService counterService) {
        this.counterService = counterService;
    }

    @PostMapping
    public ResponseEntity<CounterResponse> createCounter(@Valid @RequestBody CreateCounterRequest request) {
        CounterResponse response = counterService.createCounter(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CounterResponse>> getAllCounters() {
        return ResponseEntity.ok(counterService.getAllCounters());
    }

    @GetMapping("/active")
    public ResponseEntity<List<CounterResponse>> getActiveCounters() {
        return ResponseEntity.ok(counterService.getActiveCounters());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CounterResponse> getCounterById(@PathVariable Long id) {
        return ResponseEntity.ok(counterService.getCounterById(id));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<CounterResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(counterService.toggleCounterStatus(id));
    }
}
