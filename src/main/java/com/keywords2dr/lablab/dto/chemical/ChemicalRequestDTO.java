package com.keywords2dr.lablab.dto.chemical;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChemicalRequestDTO {

    @NotBlank(message = "Mã hóa chất (Item Code) không được để trống!")
    private String itemCode;

    @NotBlank(message = "Tên hóa chất không được để trống!")
    private String name;

    @NotBlank(message = "Đơn vị đo lường (unit) không được để trống!")
    private String unit;

    private String formula;
    private String packaging;

    @NotNull(message = "Dung tích/Khối lượng 1 chai không được để trống!")
    @DecimalMin(value = "0.01", message = "Dung lượng 1 chai phải lớn hơn 0!")
    private BigDecimal amountPerPackage;

    private String supplier;

    private Integer packageCount;
    private String roomName;
}