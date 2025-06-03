package com.team.berp.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.InventoryLog;
import com.team.berp.domain.LogStatus;
import com.team.berp.domain.LogType;

public interface Order_InventoryLogRepositroy extends JpaRepository<InventoryLog, Long> {
	 /**
     * 해당 orderLineItemId에 대해
     * log_type = 'OUT' 이고, log_status = 'CONFIRMED' 인 레코드가 존재하는지 여부
     */
	boolean existsByOrderLineItem_OrderLineItemIdAndLogTypeAndLogStatus(
	        Long orderLineItemId,
	        LogType logType,
	        LogStatus logStatus
	    );
	
	
    
    
	

}
