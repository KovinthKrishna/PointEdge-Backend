package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {
}
