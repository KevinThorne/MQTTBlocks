package me.kevinthorne.MQTTBlocks.blocks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import me.kevinthorne.MQTTBlocks.BlockManager;

public abstract class MQTTBlock extends Thread implements MqttCallback {

  Logger logger = Logger.getLogger(BlockManager.class.getName());

  protected BlockManager parent;
  protected BlockConfigurationFile blockConfig;
  protected String name;

  private String[] topics;
  private int qos = 1;
  private String broker;
  private String clientId;
  private MemoryPersistence persistence = new MemoryPersistence();

  private MqttClient client;

  private boolean running;

  private Map<String, MqttMessage> lastPublished = new HashMap<String, MqttMessage>();

  @Override
  public void interrupt() {
    try {
      onDisable();
    } catch (Exception ignored) {
    }
    try {
      client.disconnect();
      client.close();
    } catch (Exception e) {
      logWarn("Could not close client gracefully, using force...");
      try {
        client.disconnectForcibly();
        client.close();
        logWarn("Stack Trace for forced connection close:");
        e.printStackTrace();
      } catch (MqttException ignored) {
      } catch (NullPointerException e1) {
        logInfo("No client was ever created");
      }
      
    }

    super.interrupt();
  }

  public void init(BlockManager parent, BlockConfigurationFile config) {
    this.blockConfig = config;
    this.parent = parent;

    this.name = config.getName();
    this.topics = config.getTopics();
    this.broker = config.getBroker();
    this.clientId = config.getClientId();
  }

  /**
   * Called when component is started; ran in its own thread.
   */
  public abstract void onEnable();

  /**
   * Called when component is stopped; ran in its own thread.
   */
  public abstract void onDisable();

  /**
   * Component lifecycle
   * <ul>
   * <li>Setup MqttClient Object</li>
   * <li>Run onEnable</li>
   * <li>Update loop</li>
   * <li>--------------</li>
   * <li>onDisable called on <strong>interrupt</strong>
   * </ul>
   */
  public void run() {
    try {
      client = new MqttClient(broker, clientId, persistence);
      MqttConnectOptions connOpts = new MqttConnectOptions();
      connOpts.setCleanSession(true);
      if (blockConfig.getUsername() != null && !blockConfig.getUsername().equals(""))
        connOpts.setUserName(blockConfig.getUsername());
      if (blockConfig.getPassword() != null && !blockConfig.getPassword().equals(""))
        connOpts.setPassword(blockConfig.getPassword());
      client.connect(connOpts);
      for (String topic : blockConfig.getSubscribedTopics()) {
        client.subscribe(topic);
      }
      client.setCallback(this);
      running = true;
    } catch (MqttException | IllegalArgumentException e) {
      BlockManager.logError(this, "Fatal Error! Could not setup MQTT Client:");
      e.printStackTrace();
      try {
        this.interrupt();
      } catch (Exception e1) {
        e1.printStackTrace();
      }
      return;
    }
    try {
      onEnable();
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    while (running) {
      update();
      try {
        Thread.sleep(getSleepTime() * 1000);
      } catch (InterruptedException ignored) {
      }
    }
  }

  /**
   * Called every x seconds where x is getSleepTime()
   */
  public abstract void update();

  /**
   * Helper method for ease of publishing to a topic
   * 
   * @param topic
   * @param content
   */
  public void publish(String topic, String content) {
    if (client != null && client.isConnected()) {
      MqttMessage message = new MqttMessage(content.getBytes());
      message.setQos(getQos());
      try {
        lastPublished.put(topic, message);
        client.publish(topic, message);
      } catch (MqttException e) {
        BlockManager.logError(this,
            "Couldn't publish message on Thread: " + getBlockName());
        e.printStackTrace();
      }
    }
  }

  /**
   * The actual MqttClient callback function, can be overridden.
   * 
   * @param topic
   * @param message - MqttMessage type
   */
  @Override
  public void messageArrived(String topic, MqttMessage message) throws Exception {
    boolean fromHome = false;
    if (lastPublished.containsKey(topic)
        && lastPublished.get(topic).toString().equals(message.toString())) {
      fromHome = true;
      lastPublished.remove(topic);
    }
    onMessageReceived(topic, message, message.toString(), message.getQos(), message.isDuplicate(),
        message.isRetained(), fromHome);
  }


  /**
   * Fired when message is received on subscribed topics
   * 
   * @param topic
   * @param mqttMessage - Can be cast to MqttMessage
   * @param message
   * @param QoS
   * @param isDuplicate
   * @param isRetained
   * @param fromHome
   * 
   * @return message handled
   */
  public abstract boolean onMessageReceived(String topic, Object mqttMessage, String message,
      int qos, boolean isDuplicate, boolean isRetained, boolean fromHome);

  @Override
  public void connectionLost(Throwable cause) {
    logError("Connection Lost!");
    cause.printStackTrace();
    this.interrupt();
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken arg0) {
  }

  public BlockManager getParent() {
    return parent;
  }

  public BlockConfigurationFile getBlockConfig() {
    return blockConfig;
  }

  public String getBlockName() {
    return name;
  }

  public String[] getTopics() {
    return topics;
  }

  public String getBroker() {
    return broker;
  }

  public int getQos() {
    return qos;
  }

  public void setQos(int qos) {
    this.qos = qos;
  }

  public String getClientId() {
    return clientId;
  }

  public MemoryPersistence getPersistence() {
    return persistence;
  }

  public MqttClient getClient() {
    return client;
  }

  /**
   * Return how often to call update();
   * 
   * @return int <strong>seconds</strong>
   */
  public int getSleepTime() {
    return blockConfig.getUpdateWait();
  }

  public boolean isRunning() {
    return running;
  }

  protected void setRunning(boolean given) {
    running = given;
  }

  public void logError(String log) {
    BlockManager.logError(this, log);
  }

  public void logInfo(String log) {
    BlockManager.logInfo(this, log);
  }

  public void logWarn(String log) {
    BlockManager.logWarn(this, log);
  }

  public void logConfig(String log) {
    BlockManager.logConfig(this, log);
  }

}
