package com.tuhospedaje.email.outbox;

import com.tuhospedaje.service.impl.EmailOutboxDispatcher;
import com.tuhospedaje.service.impl.EmailOutboxScheduler;
import com.tuhospedaje.service.impl.EmailOutboxTransactionService;
import com.tuhospedaje.enums.EmailOutboxType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailOutboxSchedulerTest {

    private final EmailOutboxDispatcher dispatcher = mock(EmailOutboxDispatcher.class);
    private final EmailOutboxTransactionService transactions = mock(EmailOutboxTransactionService.class);
    private final ObjectProvider<EmailOutboxDispatcher> dispatcherProvider = mock(ObjectProvider.class);
    private final EmailOutboxScheduler scheduler = new EmailOutboxScheduler(dispatcherProvider, transactions);

    @Test
    void triggersWelcomeDispatchIndependently() {
        when(dispatcherProvider.getIfAvailable()).thenReturn(dispatcher);

        scheduler.pollWelcome();

        verify(dispatcher).dispatch(EmailOutboxType.WELCOME);
    }

    @Test
    void givesEverySupportedTypeOneDispatchOpportunityPerPoll() {
        when(dispatcherProvider.getIfAvailable()).thenReturn(dispatcher);

        scheduler.poll();

        for (EmailOutboxType type : EmailOutboxType.values()) {
            verify(dispatcher).dispatch(type);
        }
    }

    @Test
    void isolatesCleanupFailureFromLaterDispatches() {
        when(dispatcherProvider.getIfAvailable()).thenReturn(dispatcher);
        doThrow(new RuntimeException("database unavailable")).when(transactions).cleanup(EmailOutboxType.WELCOME);

        assertThatCode(scheduler::cleanup).doesNotThrowAnyException();
        scheduler.pollWelcome();

        verify(dispatcher).dispatch(EmailOutboxType.WELCOME);
    }

    @Test
    void retainsIndependentCleanupForEverySupportedType() {
        scheduler.cleanup();

        for (EmailOutboxType type : EmailOutboxType.values()) {
            verify(transactions).cleanup(type);
        }
    }

    @Test
    void preservesDurableWorkWhenSmtpDispatcherIsUnavailable() {
        when(dispatcherProvider.getIfAvailable()).thenReturn(null);

        assertThatCode(scheduler::pollWelcome).doesNotThrowAnyException();
    }
}
