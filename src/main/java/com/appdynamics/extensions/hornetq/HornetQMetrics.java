/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */
package com.appdynamics.extensions.hornetq;


import com.appdynamics.extensions.util.metrics.Metric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HornetQMetrics {

    private String displayName;
    private Map<String, Object> metrics;
    private List<Metric> allMetrics;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Map<String, Object> getMetrics() {
        if (metrics == null) {
            metrics = new HashMap<String, Object>();
        }
        return metrics;
    }

    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
    }

    public List<Metric> getAllMetrics() {
        if (allMetrics == null) {
            allMetrics = new ArrayList<Metric>();
        }
        return allMetrics;
    }


}
