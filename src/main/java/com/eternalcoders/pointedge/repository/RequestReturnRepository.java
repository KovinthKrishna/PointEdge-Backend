package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.RequestReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestReturnRepository extends JpaRepository<RequestReturn, Long> {
}

