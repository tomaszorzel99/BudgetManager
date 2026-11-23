package com.personalfinance.BudgetManager.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;

/**
 * Księguje transakcje cykliczne z włączonym „auto”: codziennie rano i zaraz po starcie
 * aplikacji (żeby nadrobić dni, w które aplikacja nie działała).
 */
@Configuration
@EnableScheduling
public class RecurringTransactionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionScheduler.class);

    private final RecurringTransactionService recurringService;

    public RecurringTransactionScheduler(RecurringTransactionService recurringService) {
        this.recurringService = recurringService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        processDue();
    }

    @Scheduled(cron = "${app.recurring.cron:0 15 6 * * *}")
    public void processDue() {
        LocalDate today = LocalDate.now();
        int posted = 0;
        for (Long id : recurringService.findDueForAutoPost(today)) {
            try {
                posted += recurringService.autoPost(id, today);
            } catch (RuntimeException e) {
                log.warn("Auto-posting recurring transaction {} failed, deactivating it: {}", id, e.getMessage());
                recurringService.deactivate(id);
            }
        }
        if (posted > 0) {
            log.info("Auto-posted {} recurring transaction(s)", posted);
        }
    }
}
