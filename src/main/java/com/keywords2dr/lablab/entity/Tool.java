package com.keywords2dr.lablab.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tools")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Tool extends Item {
    private String supplier;
}