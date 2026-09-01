package com.campusqueue.controller;

import com.campusqueue.dto.request.CreateTicketRequest;
import com.campusqueue.dto.response.QueueStatusResponse;
import com.campusqueue.dto.response.TicketResponse;
import com.campusqueue.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * 1. Take a ticket
     */
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse response = ticketService.createTicket(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 2. Get queue status for a counter
     */
    @GetMapping("/counter/{counterId}/status")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(@PathVariable Long counterId) {
        return ResponseEntity.ok(ticketService.getQueueStatus(counterId));
    }

    /**
     * 3. Call next student
     */
    @PostMapping("/counter/{counterId}/call-next")
    public ResponseEntity<TicketResponse> callNextTicket(@PathVariable Long counterId) {
        return ResponseEntity.ok(ticketService.callNextTicket(counterId));
    }

    /**
     * 4. Complete ticket
     */
    @PatchMapping("/{id}/complete")
    public ResponseEntity<TicketResponse> completeTicket(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ticketService.completeTicket(id, remarks));
    }

    /**
     * 5. Skip ticket
     */
    @PatchMapping("/{id}/skip")
    public ResponseEntity<TicketResponse> skipTicket(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ticketService.skipTicket(id, remarks));
    }

    /**
     * Cancel ticket
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<TicketResponse> cancelTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.cancelTicket(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TicketResponse>> getTicketsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ticketService.getTicketsByUser(userId));
    }

    @GetMapping("/counter/{counterId}")
    public ResponseEntity<List<TicketResponse>> getTicketsByCounter(@PathVariable Long counterId) {
        return ResponseEntity.ok(ticketService.getTicketsByCounter(counterId));
    }

    @GetMapping("/counter/{counterId}/waiting")
    public ResponseEntity<List<TicketResponse>> getWaitingTicketsByCounter(@PathVariable Long counterId) {
        return ResponseEntity.ok(ticketService.getWaitingTicketsByCounter(counterId));
    }

    @GetMapping("/counter/{counterId}/current")
    public ResponseEntity<TicketResponse> getCurrentlyCalledTicket(@PathVariable Long counterId) {
        return ticketService.getCurrentlyCalledTicket(counterId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
