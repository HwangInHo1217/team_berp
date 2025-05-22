package com.team.berp.mrp.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "stock")
@Data
public class DomainStock {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer stockId;

    private Integer itemId;

    private Integer warehouseId;

    private Integer quantity;
}
