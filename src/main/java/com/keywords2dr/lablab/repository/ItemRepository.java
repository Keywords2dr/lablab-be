package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    boolean existsByItemCode(String itemCode);
}