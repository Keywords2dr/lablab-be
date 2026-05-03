package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.chemical.GlobalInventoryResponse;
import java.util.List;

public interface InventoryService {
    List<GlobalInventoryResponse> getGlobalChemicalInventory();
}