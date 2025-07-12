package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.RequestReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.eternalcoders.pointedge.enums.RequestStatus;

import java.util.List;

@Repository
public interface RequestReturnRepository extends JpaRepository<RequestReturn, Long> {
    List<RequestReturn> findByStatus(RequestStatus status);

    List<RequestReturn> findByInvoice_InvoiceNumber(String invoiceNumber);

}

