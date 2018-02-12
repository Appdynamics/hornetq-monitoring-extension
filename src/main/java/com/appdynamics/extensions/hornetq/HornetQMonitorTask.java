/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */
package com.appdynamics.extensions.hornetq;


import com.appdynamics.extensions.hornetq.config.HornetQMBeanKeyPropertyEnum;
import com.appdynamics.extensions.hornetq.config.Server;
import com.appdynamics.extensions.jmx.JMXConnectionConfig;
import com.appdynamics.extensions.jmx.JMXConnectionUtil;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;

import javax.management.MBeanAttributeInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class HornetQMonitorTask implements Callable<HornetQMetrics> {

    public static final String METRICS_SEPARATOR = "|";
    public static final Logger logger = Logger.getLogger(HornetQMonitorTask.class);
    private Server server;
    private String mbeanDomainName;
    private JMXConnectionUtil jmxConnector;

    public HornetQMonitorTask(Server server, String mbeanDomainName) {
        this.server = server;
        this.mbeanDomainName = mbeanDomainName;
    }

    /**
     * Connects to a remote/local JMX server, applies exclusion filters and collects the metrics
     *
     * @return HornetQMetrics. In case of exception, the HornetQMonitorConstants.METRICS_COLLECTION_SUCCESSFUL is set with HornetQMonitorConstants.ERROR_VALUE.
     * @throws Exception
     */
    public HornetQMetrics call() throws Exception {
        HornetQMetrics hornetQMetrics = new HornetQMetrics();
        hornetQMetrics.setDisplayName(server.getDisplayName());
        try {
            if(Strings.isNullOrEmpty(server.getJmxServiceUrl())) {
                jmxConnector = new JMXConnectionUtil(new JMXConnectionConfig(server.getHost(), server.getPort(), server.getUsername(), server.getPassword()));
            }
            else {
                jmxConnector = new JMXConnectionUtil(new JMXConnectionConfig(server.getJmxServiceUrl(), server.getUsername(), server.getPassword()));
            }
            JMXConnector connector = jmxConnector.connect();
            if (connector != null) {
                Set<ObjectInstance> allMbeans = jmxConnector.getAllMBeans();
                if (allMbeans != null) {
                    Map<String, Object> filteredMetrics = gatherMetrics(allMbeans);
                    filteredMetrics.put(HornetQMonitorConstants.METRICS_COLLECTION_SUCCESSFUL, HornetQMonitorConstants.SUCCESS_VALUE);
                    hornetQMetrics.setMetrics(filteredMetrics);
                }
            }
        } catch (Exception e) {
            logger.error("Error JMX-ing into the server :: " + hornetQMetrics.getDisplayName(), e);
            hornetQMetrics.getMetrics().put(HornetQMonitorConstants.METRICS_COLLECTION_SUCCESSFUL, HornetQMonitorConstants.ERROR_VALUE);
        } finally {
            jmxConnector.close();
        }
        return hornetQMetrics;
    }

    private Map<String, Object> gatherMetrics(Set<ObjectInstance> allMbeans) {
        Map<String, Object> allMetrics = new HashMap<String, Object>();
        for (ObjectInstance mbean : allMbeans) {
            ObjectName objectName = mbean.getObjectName();
            //consider only the the metric domains (org.hornetq) mentioned in the config
            if (isDomainConfigured(objectName)) {
                MBeanAttributeInfo[] attributes = jmxConnector.fetchAllAttributesForMbean(objectName);
                if (attributes != null) {
                    for (MBeanAttributeInfo attr : attributes) {
                        try {
                            // See we do not violate the security rules, i.e. only if the attribute is readable.
                            if (attr.isReadable()) {
                                Object attribute = jmxConnector.getMBeanAttribute(objectName, attr.getName());
                                //AppDynamics only considers number values
                                if (attribute != null && attribute instanceof Number) {
                                    String metricKey = getMetricsKey(objectName, attr);
                                    allMetrics.put(metricKey, attribute);
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Error fetching attribute " + attr.getName(), e);
                        }
                    }
                }
            }

        }
        return allMetrics;
    }

    private String getMetricsKey(ObjectName objectName, MBeanAttributeInfo attr) {
        // Standard jmx keys. {type, scope, name, keyspace, path etc.}
        String type = objectName.getKeyProperty(HornetQMBeanKeyPropertyEnum.TYPE.toString());
        String module = objectName.getKeyProperty(HornetQMBeanKeyPropertyEnum.MODULE.toString());
        String address = objectName.getKeyProperty(HornetQMBeanKeyPropertyEnum.ADDRESS.toString());
        String name = objectName.getKeyProperty(HornetQMBeanKeyPropertyEnum.NAME.toString());
        StringBuilder metricsKey = new StringBuilder();
        metricsKey.append(Strings.isNullOrEmpty(type) ? "" : type + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(module) ? "" : module + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(address) ? "" : stripQuotesIfAny(address) + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(name) ? "" : stripQuotesIfAny(name) + METRICS_SEPARATOR);
        metricsKey.append(attr.getName());

        return metricsKey.toString();
    }

    private boolean isDomainConfigured(ObjectName objectName) {
        return (objectName.getDomain().equals(mbeanDomainName));
    }

    // Added this method since with quotes, metrics not rendering properly in metric browser
    private String stripQuotesIfAny(String str) {
        if(str.contains("\"")) {
            return str.replace("\"", "");
        }
        return str;
    }

}
