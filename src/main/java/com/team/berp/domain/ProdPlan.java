// File: /Team_BERP/src/main/java/com/team/berp/domain/ProdPlan.java
package com.team.berp.domain;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
@Entity
@Table(name = "prod_plan")
@Builder
public class ProdPlan {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long planId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	@Column(name = "plan_qty", nullable = false)
	private Integer planQty;

	@Column(name = "unit")
	private String unit;

	@Column(name = "plan_date")
	private LocalDate planDate;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@Column(name = "start_date")
	private LocalDate startDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private PlanStatus status = PlanStatus.PLANNED;

	@Column(name = "priority")
	private Integer priority = 3;

	@Column(name = "manager")
	private String manager;

	@Column(name = "remark", columnDefinition = "TEXT")
	private String remark;

	// enum 정의
	public enum PlanStatus {
		PLANNED, ORDERED, COMPLETED
	}

	public void update(Item item, Integer planQty, String unit, LocalDate planDate, LocalDate dueDate, PlanStatus status,
			String manager, String remark) {
		this.item = item;
		this.planQty = planQty;
		this.unit = unit;
		this.planDate = planDate;
		this.dueDate = dueDate;
		this.status = status;
		this.manager = manager;
		this.remark = remark;
	}

}