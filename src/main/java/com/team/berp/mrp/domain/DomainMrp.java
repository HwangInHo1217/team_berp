package com.team.berp.mrp.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "mrp")
@Data
public class DomainMrp {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer mrpId;

    private Integer planId;

    private Integer itemId;

    private Integer requiredQty;
}
