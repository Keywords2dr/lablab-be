package com.keywords2dr.lablab.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "chemicals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Chemical extends Item {

    @Column(name = "formula")
    private String formula; // Công thức hóa học

    @Column(name = "packaging")
    private String packaging; // Quy cách đóng gói (thủy tinh, nhựa...)

    @Column(name = "amount_per_package")
    private BigDecimal amountPerPackage; // Dung tích chuẩn của 1 chai

    @Column(name = "supplier")
    private String supplier; // Nhà cung cấp
}