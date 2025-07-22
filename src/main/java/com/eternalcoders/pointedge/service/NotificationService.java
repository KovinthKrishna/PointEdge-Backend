package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.entity.Notification;
import com.eternalcoders.pointedge.entity.Product;
import com.eternalcoders.pointedge.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void createNotification(Product product, String message) {
        Notification n = new Notification();
        n.setProduct(product);
        n.setMessage(message);
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Transactional
    public void markRead(Long id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No such notification: " + id));
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void delete(Long id) {
        if (id == 0) {
            notificationRepository.deleteAll();
        } else {
            notificationRepository.deleteById(id);
        }
    }
}
