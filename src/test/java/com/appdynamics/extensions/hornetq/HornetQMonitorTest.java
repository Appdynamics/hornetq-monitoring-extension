/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.hornetq;

import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;

import java.util.Map;

/**
 * Created by balakrishnavadavalasa on 01/04/16.
 */
public class HornetQMonitorTest {

    @Test
    public void testHornetQMonitor() throws TaskExecutionException {
        HornetQMonitor monitor = new HornetQMonitor();
        Map<String, String> taskArgs = Maps.newHashMap();
        taskArgs.put("config-file", "src/test/resources/conf/config.yml");
        monitor.execute(taskArgs, null);
    }
}
