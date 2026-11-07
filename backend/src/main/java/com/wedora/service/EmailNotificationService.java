package com.wedora.service;

import com.wedora.domain.Booking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class EmailNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);
    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String sender;

    public EmailNotificationService(JavaMailSender mailSender,
                                    @Value("${app.mail.enabled:false}") boolean enabled,
                                    @Value("${app.mail.from:notifications@wedora.local}") String sender) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.sender = sender;
    }

    public void bookingRequested(Booking booking) {
        sendAfterCommit(booking.getSupplier().getOwner().getEmail(), "A couple sent you a booking request",
                "A new wedding booking request was received for " + booking.getEventDate() + ".");
    }

    public void quoteReceived(Booking booking) {
        sendAfterCommit(booking.getCustomer().getEmail(), "Your supplier sent a quote",
                booking.getSupplier().getBusinessName() + " sent a quote for your wedding on " + booking.getEventDate() + ".");
    }

    public void supplierAccepted(Booking booking) {
        sendAfterCommit(booking.getCustomer().getEmail(), "Your supplier accepted your request",
                booking.getSupplier().getBusinessName() + " accepted your request for " + booking.getEventDate() + ".");
    }

    public void bookingConfirmed(Booking booking) {
        sendAfterCommit(booking.getSupplier().getOwner().getEmail(), "A couple confirmed your quote",
                "Your booking for " + booking.getEventDate() + " has been confirmed.");
    }

    private void sendAfterCommit(String recipient, String subject, String body) {
        if (!enabled || recipient.endsWith("@account.wedmaster.invalid")) return;
        Runnable delivery = () -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender);
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            try {
                mailSender.send(message);
            } catch (MailException exception) {
                logger.error("Wedding email notification delivery failed", exception);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delivery.run();
                }
            });
        } else {
            delivery.run();
        }
    }
}
