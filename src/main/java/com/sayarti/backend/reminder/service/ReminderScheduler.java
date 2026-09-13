package com.sayarti.backend.reminder.service;

import com.sayarti.backend.reminder.repository.ReminderRepository;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private final ReminderRepository reminders;
    private final ReminderNotificationProcessor processor;
    private final int batchSize;
    private final Clock clock = Clock.systemUTC();

    public ReminderScheduler(ReminderRepository reminders,
            ReminderNotificationProcessor processor,
            @Value("${sayarti.reminder-scheduler.batch-size:100}") int batchSize) {
        this.reminders = reminders;
        this.processor = processor;
        this.batchSize = Math.max(1, batchSize);
    }

    @Scheduled(fixedDelayString = "${sayarti.reminder-scheduler.fixed-delay-ms:60000}")
    public void poll() {
        for (UUID reminderId : reminders.findDueReminderIds(clock.instant(),
                PageRequest.of(0, batchSize))) {
            try {
                processor.process(reminderId);
            } catch (RuntimeException exception) {
                log.warn("Reminder notification processing failed; reminderId={}, failureType={}",
                        reminderId, exception.getClass().getSimpleName());
            }
        }
    }
}
