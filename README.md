# MQTTComponents
###### A Light Java Client Daemon for MQTT communication

## Purpose
This small, modular, Apache-licensed application provides a simple framework to subscribe and publish on topics on any Paho-supported MQTT broker.

## Design
In a nutshell, the application works in a modular pattern. Components are loaded from the "components/" directory and will run along side with many other components. Each component, other than the core components (built-in), has it's own update thread and specified cycle.

One of the core components, the Component Loader, hotloads the JAR files in the directory. The new components are instantly imported and ran after the Loader finds them.

#### Component Design
Every component has to have two parts: a configuration file, and a class extending MQTTComponent. The configuration file then provides the information needed to set up the component properly. A sample configuration file looks like this:
```
name=Test Component
description=Component to test dynamic loading, Template component
main=me.example.test.TestComponent
topic=/test, /test/two, /test/three
qos=0
broker=tcp://localhost:1883
clientId=myComputer
username=
password=
updateWait=10
```

##### Configuration Details
This will simply explain what each field in the configuration files are for.
- ```name``` - Defines the name of your component, you will see this in log files mostly.
- ```description``` - Explains what your component does for others to read.
- ```main``` - Full package name where your MQTTComponent class is located.
- ```topic``` - One or more topics to automatically be subscribed to on component startup.
- ```qos``` - Sets the default QoS for any messages sent out by your component.
- ```broker``` - The url read by Eclipse Paho to set up the MQTTClient in your Component.
- ```clientId``` - Client identification.
- ```username``` - Username for the connection.
- ```password``` - Password for the connection.
- ```updateWait``` - How long (in seconds) to sleep before calling ```update()``` again.

##### Component Class Details
```
package me.example.test;

import java.util.Date;

import me.kevinthorne.MQTTComponents.components.MQTTComponent;

public class TestComponent extends MQTTComponent {

  @Override
  public void onDisable() {
    logInfo("Test Component disabled");
  }

  @Override
  public void onEnable() {
    logInfo("Test Component enabled");
  }

  @Override
  public void update() {
    logInfo("Test Component update cycle");
    publish(getTopics()[0], new Date().toString());
    publish(getTopics()[2], new Date().toString());
  }

  @Override
  public boolean onMessageReceived(String topic, Object mqttMessage, String message, int qos,
      boolean isDuplicate, boolean isRetained, boolean fromHome) {
    if (!fromHome)
      logInfo("Received: \"" + message + "\" on \"" + topic + "\"");
    return true;
  }
}
```
All the methods seen here are completely encased in their own threads.
