package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
