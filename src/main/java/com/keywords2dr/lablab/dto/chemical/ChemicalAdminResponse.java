package com.keywords2dr.lablab.dto.chemical;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ChemicalAdminResponse {
    private UUID itemId;
    private String itemCode;
    private String name;
    private String formula;
    private String unit;
    private String packaging;
    private BigDecimal amountPerPackage;
    private String supplier;
}