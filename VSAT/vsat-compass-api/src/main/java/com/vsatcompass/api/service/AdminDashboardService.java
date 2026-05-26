package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.response.AdminStatsResponse;

public interface AdminDashboardService {

    AdminStatsResponse getStats();
}
