package com.personalfinance.BudgetManager.Controller;

import com.personalfinance.BudgetManager.DTO.CreateTransferRequest;
import com.personalfinance.BudgetManager.DTO.TransferDTO;
import com.personalfinance.BudgetManager.Mapper.TransferMapper;
import com.personalfinance.BudgetManager.Model.Transfer;
import com.personalfinance.BudgetManager.Repositories.TransferRepository;
import com.personalfinance.BudgetManager.Services.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tranfers")
public class TransferController {

    private final TransferService transferService;
    private final TransferMapper transferMapper;

    public TransferController(TransferService transferService, TransferMapper transferMapper) {
        this.transferService = transferService;
        this.transferMapper = transferMapper;
    }

    @PostMapping
    public ResponseEntity<TransferDTO> createTransfer(@Valid @RequestBody CreateTransferRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        Transfer transfer = transferService.createTransfer(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(transferMapper.convertToDTO(transfer));
    }

    @GetMapping
    public ResponseEntity<List<TransferDTO>> getTransfers(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<Transfer> transfers = transferService.getTransfers(userDetails.getUsername(), accountId, month, year);

        return ResponseEntity.ok(transferMapper.convertToDTOList(transfers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferDTO> getTransferById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Transfer transfer = transferService.getTransferById(id, userDetails.getUsername());
        return ResponseEntity.ok(transferMapper.convertToDTO(transfer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        transferService.deleteTransfer(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
