 Hornetq Monitoring Extension
=============================
An AppDynamics extension to be used with a stand alone Java machine agent to provide metrics for HornetQ messaging server.

## Use Case ##
HornetQ is an open source project to build a multi-protocol, embeddable, very high performance, clustered, asynchronous messaging system. HornetQ is an example of Message Oriented Middleware (MoM).
This monitoring extension captures statistics from the HornetQ server and displays them in the AppDynamics Metric Browser.

## Prerequisites ##
JMX must be enabled in HornetQ Messaging server for this extension to gather metrics. Connect with jconsole to check if JMX is enabled.

## Installation ##
1. Run "mvn clean install" and find the HornetQMonitor.zip file in the "target" folder.
2. Unzip as "HornetQMonitor" and copy the "HornetQMonitor" directory to `<MACHINE_AGENT_HOME>/monitors`

## Configuration ##
Note : Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the HornetQ extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/HornetQMonitor/`. 

   For eg.
   ```
        # List of HornetQ servers
        servers:
          - host: "localhost"
            port: 3333
            username: ""
            password: ""
            displayName: "localhost"

        # hornetq mbean domain name
        mbeanDomainName: "org.hornetq"

        # number of concurrent tasks
        numberOfThreads: 10

        #timeout for the thread
        threadTimeout: 30

         #prefix used to show up metrics in AppDynamics
        metricPathPrefix:  "Custom Metrics|HornetQ|"

        #Metric Overrides. Change this if you want to transform the metric key or value or its properties.
        metricOverrides:
          - metricKey: "Acceptor.*"
            disabled: true
  
          - metricKey: "Address.*"
            disabled: true

          - metricKey: ".*Rate.*"
            postfix: "Percent"
            multiplier: 100
            disabled: false
            timeRollup: "AVERAGE"
            clusterRollup: "COLLECTIVE"
            aggregator: "SUM"
   ```
3. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/HornetQMonitor/` directory. Below is the sample

     ```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/HornetQMonitor/config.yml" />
          ....
     </task-arguments>
    ```

## Contributing ##
Always feel free to fork and contribute any changes directly here on GitHub.

## Community ##

Find out more in the [AppDynamics Exchange][].

## Support ##

For any questions or feature request, please contact [AppDynamics Center of Excellence][].

[Github]: https://github.com/Appdynamics/cassandra-monitoring-extension
[AppDynamics Exchange]: http://community.appdynamics.com/t5/AppDynamics-eXchange/idb-p/extensions
[AppDynamics Center of Excellence]: mailto:help@appdynamics.com
