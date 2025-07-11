package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.dto.*;
import com.eternalcoders.pointedge.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    List<InvoiceItem> findByInvoice_InvoiceNumber(String invoiceNumber);

}