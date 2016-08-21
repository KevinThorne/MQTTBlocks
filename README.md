# MQTTBlocks
###### A Light Java Client Daemon for MQTT communication

## Purpose
This small, modular, Apache-licensed application provides a simple framework to subscribe and publish on topics on any Paho-supported MQTT broker.

## Design
In a nutshell, the application works in a modular pattern. Components are loaded from the "blocks/" directory and will run along side with many other blocks. Each block, other than the core blocks (built-in), has it's own update thread and specified cycle.

One of the core blocks, the BlockLoader, hotloads the JAR files in the directory. The new blocks are instantly imported and ran after the Loader finds them.

#### Block Design
Every block has to have two parts: a configuration file, and a class extending [```MQTTBlock```](https://github.com/KevinThorne/MQTTBlocks/blob/master/MQTTBlocks/src/me/kevinthorne/MQTTBlocks/blocks/MQTTBlock.java). The configuration file then provides the information needed to set up the block properly. A sample configuration file looks like this:
```
name=Test Block
description=Block to test dynamic loading, Template block
main=me.example.test.TestBlock
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
- ```name``` - Defines the name of your block, you will see this in log files mostly.
- ```description``` - Explains what your block does for others to read.
- ```main``` - Full package name where your MQTTBlock class is located.
- ```topic``` - One or more topics to automatically be subscribed to on block startup.
- ```qos``` - Sets the default QoS for any messages sent out by your block.
- ```broker``` - The url read by Eclipse Paho to set up the MQTTClient in your Component.
- ```clientId``` - Client identification.
- ```username``` - Username for the connection.
- ```password``` - Password for the connection.
- ```updateWait``` - How long (in seconds) to sleep before calling ```update()``` again.

##### Block Class Details
```
package me.example.test;

import java.util.Date;

import me.kevinthorne.MQTTBlocks.blocks.MQTTBlock;

public class TestBlock extends MQTTBlock {

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
    logInfo("Test Block update cycle");
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
###### Block Class API
Here's what each of them do and when they are ran:
- ```onEnable``` - called when the component is enabled by the Block Manager. The MqttClient is instantiated by this point; any more customization of the Client object can be made here safely.
- ```onDisable``` - do any cleanup here.
- ```update``` - Called every x seconds where x is the ```updateWait``` setting.
- ```onMessageReceived``` - Called every time a message is received on any of the subscribed topics (both set in the configuration and any you subscribe to in onEnable or anywhere else.

There are a couple of other methods that the [```MQTTBlock```](https://github.com/KevinThorne/MQTTBlocks/blob/master/MQTTBlocks/src/me/kevinthorne/MQTTBlocks/blocks/MQTTBlock.java) class implements, most of which can be overridden in your code:
- ```publish(String topic, String message)``` - This will publish any given message to any given topic to the broker.
- ```init(BlockManager parent, BlockConfigurationFile config)``` - Block Loader calls this to initialize the class. **Do not override**
- ```run()``` - This is the lifeline of each component. **Do not override either.** Doing so will kill your block's lifecycle and potentially the entire application.
- The ```MqttCallback``` are also implemented in the [```MQTTBlock```](https://github.com/KevinThorne/MQTTBlocks/blob/master/MQTTBlocks/src/me/kevinthorne/MQTTBlocks/blocks/MQTTBlock.java) class:
  - ```messageArrived(String topic, MqttMessage message)``` - This is the parent method of ```onMessageReceived```. This is really what the MqttClient object calls when a message is received. However, further knowledge of Eclipse Paho is needed.
  - ```connectionLost(Throwable cause)``` - Closes down the component by default.
  - ```deliveryComplete(IMqttDeliveryToken token)``` - Empty by default.

Here are a couple of helper methods:
- ```logError(String message), logInfo(String message), logWarn(String message), logConfig(String message)``` - Logs to the main Component Manager with formatting.
- ```interrupt()``` - Tears down component and interrupts the thread.

## Download
Developing Blocks are quite easy. Soon, all you will have to do is import Paho and MQTTBlocks in your buildpath. 

*Note: When MQTTBlocks reaches it's first release, a JAR file wil be available. Soon after, it will be a part of the Maven Central Repo.*

Maven:
```
<dependency>
	<groupId>org.eclipse.paho</groupId>
	<artifactId>org.eclipse.paho.client.mqttv3</artifactId>
	<version>1.0.2</version>
</dependency>
```
## Developing
As seen above, just extend [```MQTTBlock```](https://github.com/KevinThorne/MQTTBlocks/blob/master/MQTTBlocks/src/me/kevinthorne/MQTTBlocks/blocks/MQTTBlock.java).

```public class MyComponent extends MQTTBlock {```

Then build your configuration file's ```main``` property pointing to the class.
