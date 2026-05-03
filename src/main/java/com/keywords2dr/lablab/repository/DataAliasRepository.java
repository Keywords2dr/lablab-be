package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.DataAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DataAliasRepository extends JpaRepository<DataAlias, UUID> {
}