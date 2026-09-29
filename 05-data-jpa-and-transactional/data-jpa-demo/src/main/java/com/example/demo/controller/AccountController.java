package com.example.demo.controller;

import com.example.demo.entity.Account;
import com.example.demo.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedAccounts() {
        accountService.seedData();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Accounts seeded successfully");
        response.put("count", 2);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transferMoney(@RequestBody TransferRequest request) {
        accountService.transferMoney(request.fromId(), request.toId(), request.amount());

        Account from = accountService.findById(request.fromId());
        Account to = accountService.findById(request.toId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Transfer successful");
        response.put("fromBalance", from.getBalance());
        response.put("toBalance", to.getBalance());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer-without-transaction")
    public ResponseEntity<Void> transferMoneyWithoutTransaction(@RequestBody TransferRequest request) {
        accountService.transferMoneyWithoutTransaction(
                request.fromId(), request.toId(), request.amount());
        return ResponseEntity.noContent().build();
    }

    public record TransferRequest(Long fromId, Long toId, BigDecimal amount) {
    }
}
