package com.example.demo.service;

import com.example.demo.entity.Account;
import com.example.demo.exception.InsufficientFundsException;
import com.example.demo.exception.SimulatedTransferFailureException;
import com.example.demo.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
        Account from = accountRepository.findById(fromId)
                .orElseThrow(() -> new IllegalArgumentException("From account not found"));

        Account to = accountRepository.findById(toId)
                .orElseThrow(() -> new IllegalArgumentException("To account not found"));

        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        accountRepository.save(from);
        accountRepository.save(to);
    }

    public void transferMoneyWithoutTransaction(Long fromId, Long toId, BigDecimal amount) {
        Account from = accountRepository.findById(fromId)
                .orElseThrow(() -> new IllegalArgumentException("From account not found"));
        accountRepository.findById(toId)
                .orElseThrow(() -> new IllegalArgumentException("To account not found"));

        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(amount));
        accountRepository.saveAndFlush(from);

        throw new SimulatedTransferFailureException(
                "Simulated failure after sender balance was saved; recipient was not credited");
    }

    @Transactional
    public void seedData() {
        accountRepository.save(new Account("a@example.com", new BigDecimal("1000.00")));
        accountRepository.save(new Account("b@example.com", new BigDecimal("500.00")));
    }

    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }
}
