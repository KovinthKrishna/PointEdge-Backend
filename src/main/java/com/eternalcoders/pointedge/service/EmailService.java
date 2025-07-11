package com.eternalcoders.pointedge.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendPasswordResetEmail(String toEmail, String token) {
        String subject = "Password Reset - PointEdge";
        String resetUrl = "http://localhost:5173/resetPW?token=" + token;
        String body = "Hi,\n\nClick the link below to reset your password:\n" + resetUrl + "\n\nIf you didn't request this, you can ignore this email.\n\nThanks,\nPointEdge Team";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("kenukaruna08@gmail.com"); // match with application.properties
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }
}
