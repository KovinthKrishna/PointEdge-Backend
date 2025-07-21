package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.RequestReturn;
import com.eternalcoders.pointedge.entity.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnItemRepository extends JpaRepository<ReturnItem, Long> {

}
