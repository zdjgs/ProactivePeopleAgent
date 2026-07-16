package com.proactiveperson.monitor.stub;

import com.proactiveperson.monitor.ActivityMonitor;
import com.proactiveperson.monitor.ActivityStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "pp.monitor.provider", havingValue = "stub", matchIfMissing = true)
public class IdleActivityMonitor implements ActivityMonitor {

    @Override
    public ActivityStatus currentStatus(String userId) {
        return ActivityStatus.IDLE;
    }
}
