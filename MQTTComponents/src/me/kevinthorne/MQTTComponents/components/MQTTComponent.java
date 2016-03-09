package me.kevinthorne.MQTTComponents.components;

import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import me.kevinthorne.MQTTComponents.ComponentManager;

public abstract class MQTTComponent extends Thread implements MqttCallback {

  Logger logger = Logger.getLogger(ComponentManager.class.getName());

  protected ComponentManager parent;
  protected ComponentConfigurationFile config;
  protected String name;

  private String topic;
  private int qos = 1;
  private String broker;
  private String clientId;
  private MemoryPersistence persistence = new MemoryPersistence();

  private MqttClient client;
  
  private boolean running;

  @Override
  public void interrupt() {
    stopUpdate();
    try {
      onDisable();
    } catch (Exception ignored) {
    }
    try {
      client.disconnect();
      client.close();
    } catch (MqttException e) {
      try {
        client.disconnectForcibly();
        client.close();
      } catch (MqttException ignored) {
      }

      e.printStackTrace();
    }

    super.interrupt();
  }

  public void init(ComponentManager parent, ComponentConfigurationFile config) {
    this.config = config;
    this.parent = parent;

    this.name = config.getName();
    this.topic = config.getTopic();
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
   */
  public void run() {
    try {
      onEnable();
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    try {
      client = new MqttClient(broker, clientId, persistence);
      MqttConnectOptions connOpts = new MqttConnectOptions();
      connOpts.setCleanSession(true);
      if(config.getUsername() != null && !config.getUsername().equals(""))
        connOpts.setUserName(config.getUsername());
      if(config.getPassword() != null && !config.getPassword().equals(""))
        connOpts.setPassword(config.getPassword());
      client.connect(connOpts);
      client.subscribe(getTopic());
      client.setCallback(this);
      running = true;
    } catch (MqttException | IllegalArgumentException e) {
      ComponentManager.logError(this, "Fatal Error! Could not setup MQTT Client:");
      e.printStackTrace();
      try {
        stopUpdate();
        onDisable();
      } catch (Exception e1) {
        e1.printStackTrace();
      }
      return;
    }
    while (running) {
      update();
      try {
        Thread.sleep(getSleepTime()*1000);
      } catch (InterruptedException ignored) {
      }
    }

    try {
      stopUpdate();
      onDisable();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Called every x seconds where x is getSleepTime()
   */
  public abstract void update();

  public void publish(String content) {
    if (client != null && client.isConnected()) {
      MqttMessage message = new MqttMessage(content.getBytes());
      message.setQos(getQos());
      try {
        client.publish(getTopic(), message);
      } catch (MqttException e) {
        ComponentManager.logError(this,
            "Couldn't publish message on Thread: " + getComponentName());
        e.printStackTrace();
      }
    }
  }

  @Override
  public void messageArrived(String topic, MqttMessage message) throws Exception {
    onMessageReceived(topic, message, message.toString(), message.getQos(), message.isDuplicate(),
        message.isRetained());
  }

  public void stopUpdate() {
    running = false;
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
   * 
   * @return message handled
   */
  public abstract boolean onMessageReceived(String topic, Object mqttMessage, String message, int qos,
      boolean isDuplicate, boolean isRetained);

  @Override
  public void connectionLost(Throwable arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken arg0) {
    // TODO Auto-generated method stub
  }

  public ComponentManager getParent() {
    return parent;
  }

  public ComponentConfigurationFile getConfig() {
    return config;
  }

  public String getComponentName() {
    return name;
  }

  public String getTopic() {
    return topic;
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
   * @return int seconds
   */
  public abstract int getSleepTime();

  public boolean isRunning() {
    return running;
  }
  protected void setRunning(boolean given) {
    running = given;
  }

  public void logError(String log) {
    ComponentManager.logError(this, log);
  }

  public void logInfo(String log) {
    ComponentManager.logInfo(this, log);
  }

  public void logWarn(String log) {
    ComponentManager.logWarn(this, log);
  }

  public void logConfig(String log) {
    ComponentManager.logConfig(this, log);
  }

}
