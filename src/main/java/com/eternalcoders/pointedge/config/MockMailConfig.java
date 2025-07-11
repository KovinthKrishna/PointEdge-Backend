package com.eternalcoders.pointedge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@Profile("dev")
public class MockMailConfig {

    @Bean
    public JavaMailSenderImpl javaMailSender() {
        return new JavaMailSenderImpl() {
            @Override
            public void send(SimpleMailMessage message) {
                System.out.println("🚀 Mock email sent to: " + String.join(", ", message.getTo()));
                System.out.println("📬 Subject: " + message.getSubject());
                System.out.println("📝 Body:\n" + message.getText());
            }

            @Override
            public void send(SimpleMailMessage... messages) {
                for (SimpleMailMessage message : messages) {
                    send(message);
                }
            }
        };
    }
}