/**
 * Copyright 2015 AppDynamics
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdynamics.extensions.hornetq;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.hornetq.config.Configuration;
import com.appdynamics.extensions.hornetq.config.Server;
import com.appdynamics.extensions.util.metrics.Metric;
import com.appdynamics.extensions.util.metrics.MetricFactory;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * This extension will extract out metrics from HornetQ through the JMX protocol.
 */
public class HornetQMonitor extends AManagedMonitor {

    public static final Logger logger = Logger.getLogger(HornetQMonitor.class);
    public static final String CONFIG_ARG = "config-file";
    public static final String METRIC_SEPARATOR = "|";
    public static final int DEFAULT_THREAD_TIMEOUT = 10;
    private static final int DEFAULT_NUMBER_OF_THREADS = 10;
    private ExecutorService threadPool;

    public HornetQMonitor() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);

    }

    public static String getImplementationVersion() {
        return HornetQMonitor.class.getPackage().getImplementationTitle();
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        if (taskArgs != null) {
            logger.info("Starting the HornetQ Monitoring task.");
            if (logger.isDebugEnabled()) {
                logger.debug("Task Arguments Passed ::" + taskArgs);
            }
            String configFilename = getConfigFilename(taskArgs.get(CONFIG_ARG));
            try {
                //read the config.
                Configuration config = YmlReader.readFromFile(configFilename, Configuration.class);
                threadPool = Executors.newFixedThreadPool(config.getNumberOfThreads() == 0 ? DEFAULT_NUMBER_OF_THREADS : config.getNumberOfThreads());
                List<Future<HornetQMetrics>> parallelTasks = createConcurrentTasks(config);
                //collect the metrics
                List<HornetQMetrics> hornetQMetrics = collectMetrics(parallelTasks, config.getThreadTimeout() == 0 ? DEFAULT_THREAD_TIMEOUT : config.getThreadTimeout());
                // to override and print metrics
                MetricFactory<Object> metricFactory = new MetricFactory<Object>(config.getMetricOverrides());
                for (HornetQMetrics hornetQMetric : hornetQMetrics) {
                    hornetQMetric.getAllMetrics().addAll(metricFactory.process(hornetQMetric.getMetrics()));
                }
                for (HornetQMetrics hornetQMetric : hornetQMetrics) {
                    printMetrics(hornetQMetric.getAllMetrics(), config, hornetQMetric);
                }

                logger.info("HornetQ monitoring task completed successfully.");
                return new TaskOutput("HornetQ monitoring task completed successfully.");
            } catch (Exception e) {
                logger.error("Metrics collection failed ", e);
            } finally {
                if (!threadPool.isShutdown()) {
                    threadPool.shutdown();
                }
            }

        }
        throw new TaskExecutionException("HornetQ monitoring task completed with failures.");
    }

    private void printMetrics(List<Metric> allMetrics, Configuration configuration, HornetQMetrics hornetQMetric) {
        String prefix = getMetricPathPrefix(configuration, hornetQMetric);
        for (Metric aMetric : allMetrics) {
            printMetric(prefix + aMetric.getMetricPath(), aMetric.getMetricValue().toString(), aMetric.getAggregator(), aMetric.getTimeRollup(), aMetric.getClusterRollup());
        }
    }

    private String getMetricPathPrefix(Configuration config, HornetQMetrics cMetric) {
        return config.getMetricPathPrefix() + cMetric.getDisplayName() + METRIC_SEPARATOR;
    }

    /**
     * Creates concurrent tasks
     *
     * @param config
     * @return Handles to concurrent tasks.(
     */
    private List<Future<HornetQMetrics>> createConcurrentTasks(Configuration config) {
        List<Future<HornetQMetrics>> parallelTasks = new ArrayList<Future<HornetQMetrics>>();
        if (config != null && config.getServers() != null) {
            for (Server server : config.getServers()) {
                HornetQMonitorTask hornetQMonitorTask = new HornetQMonitorTask(server, config.getMbeanDomainName());
                parallelTasks.add(getThreadPool().submit(hornetQMonitorTask));
            }
        }
        return parallelTasks;
    }

    /**
     * Collects the result from the thread.
     *
     * @param parallelTasks
     * @return
     */
    private List<HornetQMetrics> collectMetrics(List<Future<HornetQMetrics>> parallelTasks, int timeout) {
        List<HornetQMetrics> allMetrics = new ArrayList<HornetQMetrics>();
        for (Future<HornetQMetrics> aParallelTask : parallelTasks) {
            HornetQMetrics hornetQMetric = null;
            try {
                hornetQMetric = aParallelTask.get(timeout, TimeUnit.SECONDS);
                allMetrics.add(hornetQMetric);
            } catch (InterruptedException e) {
                logger.error("Task interrupted. ", e);
            } catch (ExecutionException e) {
                logger.error("Task execution failed. ", e);
            } catch (TimeoutException e) {
                logger.error("Task timed out. ", e);
            }
        }
        return allMetrics;
    }

    /**
     * A helper method to report the metrics.
     *
     * @param metricPath
     * @param metricValue
     * @param aggType
     * @param timeRollupType
     * @param clusterRollupType
     */
    private void printMetric(String metricPath, String metricValue, String aggType, String timeRollupType, String clusterRollupType) {
        MetricWriter metricWriter = getMetricWriter(metricPath,
                aggType,
                timeRollupType,
                clusterRollupType
        );
        //  System.out.println("Sending [" + aggType + METRIC_SEPARATOR + timeRollupType + METRIC_SEPARATOR + clusterRollupType
        //              + "] metric = " + metricPath + " = " + metricValue);
        if (logger.isDebugEnabled()) {
            logger.debug("Sending [" + aggType + METRIC_SEPARATOR + timeRollupType + METRIC_SEPARATOR + clusterRollupType
                    + "] metric = " + metricPath + " = " + metricValue);
        }
        metricWriter.printMetric(metricValue);
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    /**
     * Returns a config file name,
     *
     * @param filename
     * @return String
     */
    private String getConfigFilename(String filename) {
        if (filename == null) {
            return "";
        }
        //for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }
        //for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if (!Strings.isNullOrEmpty(filename)) {
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
    }

}
