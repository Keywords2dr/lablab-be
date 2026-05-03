package com.keywords2dr.lablab.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "assets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Asset extends Item {
    private BigDecimal originalPrice;
    private String supplier;
}