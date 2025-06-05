package com.team.berp.index;

import java.util.List;

import com.team.berp.domain.InventoryLog;

public interface Index_DashboardService {
	List<InventoryLog> getLatestInLogs(int limit);
	List<InventoryLog> getLatestOutLogs(int limit);
	
	
}
