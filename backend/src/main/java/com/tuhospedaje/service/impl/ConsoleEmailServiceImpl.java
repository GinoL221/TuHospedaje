package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsoleEmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailServiceImpl.class);

    @Override
    public void send(EmailMessage message) {
        log.info("email.delivery event_type={} aggregate_id={} subject={}",
                message.emailType(), message.aggregateId(), message.subject());
    }
}
