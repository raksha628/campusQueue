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
     * 1. Create / Take a ticket
     */
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse response = ticketService.createTicket(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 2. Retrieve ticket by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    /**
     * 3. Call a specific ticket by ID
     */
    @PostMapping("/{id}/call")
    public ResponseEntity<TicketResponse> callTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.callTicket(id));
    }

    /**
     * 4. Call the next waiting ticket for a counter in FIFO order
     */
    @PostMapping("/counter/{counterId}/call-next")
    public ResponseEntity<TicketResponse> callNextTicket(@PathVariable Long counterId) {
        return ResponseEntity.ok(ticketService.callNextTicket(counterId));
    }

    /**
     * 5. Complete ticket
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<TicketResponse> completeTicket(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ticketService.completeTicket(id, remarks));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<TicketResponse> completeTicketPatch(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ticketService.completeTicket(id, remarks));
    }

    /**
     * 6. Skip ticket
     */
    @PostMapping("/{id}/skip")
    public ResponseEntity<TicketResponse> skipTicket(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ticketService.skipTicket(id, remarks));
    }

    @PatchMapping("/{id}/skip")
    public ResponseEntity<TicketResponse> skipTicketPatch(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ticketService.skipTicket(id, remarks));
    }

    /**
     * 7. Cancel ticket
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<TicketResponse> cancelTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.cancelTicket(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<TicketResponse> cancelTicketPatch(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.cancelTicket(id));
    }

    /**
     * 8. Live queue status for a counter
     */
    @GetMapping("/counter/{counterId}/status")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(@PathVariable Long counterId) {
        return ResponseEntity.ok(ticketService.getQueueStatus(counterId));
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
