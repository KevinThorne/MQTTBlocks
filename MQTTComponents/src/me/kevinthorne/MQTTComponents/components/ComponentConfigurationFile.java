package me.kevinthorne.MQTTComponents.components;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ComponentConfigurationFile {
  
  private String name;
  private String description;
  private String main;
  private String topic;
  private int qos = 1;
  private String broker;
  private String username;
  private char[] password;
  private String clientId;
  
  /**
   * Loads Configuration File from Jar Entry InputStream
   * 
   * @param stream
   * @throws IOException
   */
  public ComponentConfigurationFile(final InputStream stream) throws IOException {
    Properties prop = new Properties();
    prop.load(stream);
    load(prop);
  }
  
  /**
   * Loads Configuration File from given parameters, used for
   * embedded and self-registered components.
   * 
   * @param name
   * @param description
   * @param main
   * @param topic
   * @param qos
   * @param broker
   * @param clientId
   */
  public ComponentConfigurationFile(String name, String description, String main, String topic, int qos, String broker, String username, String password, String clientId) {
    this.name = name;
    this.description = description;
    this.main = main;
    this.topic = topic;
    this.qos = qos;
    this.broker = broker;
    this.username = username;
    this.password = (password == null ? "".toCharArray() : password.toCharArray());
    this.clientId = clientId;
  }
  
  private void load(Properties prop) {
    this.name = prop.getProperty("name");
    this.description = prop.getProperty("description");
    this.main = prop.getProperty("main");
    this.topic = prop.getProperty("topic");
    this.qos = Integer.parseInt(prop.getProperty("qos"));
    this.broker = prop.getProperty("broker");
    this.username = prop.getProperty("username", "");
    this.password = prop.getProperty("password", "").toCharArray();
    this.clientId = prop.getProperty("clientId");
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getMain() {
    return main;
  }
  
  public String getTopic() {
    return topic;
  }

  public int getQos() {
    return qos;
  }

  public String getBroker() {
    return broker;
  }

  public String getClientId() {
    return clientId;
  }

  public String getUsername() {
    return username;
  }

  public char[] getPassword() {
    return password;
  }

}
