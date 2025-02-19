package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
