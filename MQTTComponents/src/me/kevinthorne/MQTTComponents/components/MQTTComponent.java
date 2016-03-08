package me.kevinthorne.MQTTComponents.components;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import me.kevinthorne.MQTTComponents.ComponentManager;

public abstract class MQTTComponent implements Runnable, MqttCallback {

  Logger logger = Logger.getLogger(ComponentManager.class.getName());

  protected ComponentManager parent;
  protected ComponentConfigurationFile config;

  private String topic;
  private int qos = 1;
  private String broker;
  private String clientId;
  private MemoryPersistence persistence = new MemoryPersistence();
  
  Future future;
  MqttClient client;
  String name;

  public void init(ComponentManager parent, ComponentConfigurationFile config) {
    this.parent = parent;
    this.config = config;

    this.name = config.getName();
    this.topic = config.getTopic();
    this.broker = config.getBroker();
    this.clientId = config.getClientId();

    try {
      client = new MqttClient(broker, clientId, persistence);
      MqttConnectOptions connOpts = new MqttConnectOptions();
      connOpts.setCleanSession(true);
      client.connect(connOpts);
      client.subscribe(getTopic());
      client.setCallback(this);
    } catch (MqttException e) {
      ComponentManager.logError(this, "Fatal Error! Could not connect to broker");
      e.printStackTrace();
    }
  }
  
  public void startThread() {
    future = Executors.newSingleThreadExecutor().submit(this);
  }
  public void stopThread(boolean mayInterruptIfRunning) {
    future.cancel(mayInterruptIfRunning);
  }

  public void destroy() {
    try {
      onDisable();
      future.cancel(true);
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
  }

  public abstract void onEnable() throws Exception;

  public abstract void onDisable() throws Exception;

  public abstract void run();

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
    onMessageReceived(topic, message, message.toString(), message.getQos(), message.isDuplicate(), message.isRetained());
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
   */
  public abstract void onMessageReceived(String topic, Object mqttMessage, String message, int qos, boolean isDuplicate, boolean isRetained);
  
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

}
