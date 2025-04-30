package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.ReturnRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnRecordRepository extends JpaRepository<ReturnRecord, Long> {
    List<ReturnRecord> findByInvoiceNumber(String invoiceNumber);
}
