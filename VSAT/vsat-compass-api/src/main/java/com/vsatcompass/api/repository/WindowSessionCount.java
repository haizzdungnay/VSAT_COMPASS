package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.enums.SessionStatus;

public interface WindowSessionCount {
    Long getWindowId();
    SessionStatus getStatus();
    Long getCount();
}
