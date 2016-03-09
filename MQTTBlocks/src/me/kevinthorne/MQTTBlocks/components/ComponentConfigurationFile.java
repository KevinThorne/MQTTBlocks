package me.kevinthorne.MQTTBlocks.components;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ComponentConfigurationFile {
  
  private String name;
  private String description;
  private String main;
  private String[] topics;
  private int qos = 1;
  private String broker;
  private String username;
  private char[] password;
  private String clientId;
  private int updateWait;
  
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
  public ComponentConfigurationFile(String name, String description, String main, String[] topics, int qos, String broker, String username, String password, String clientId, int updateWait) {
    this.name = name;
    this.description = description;
    this.main = main;
    this.topics = topics;
    this.qos = qos;
    this.broker = broker;
    this.username = username;
    this.password = (password == null ? "".toCharArray() : password.toCharArray());
    this.clientId = clientId;
    this.updateWait = updateWait;
  }
  
  private void load(Properties prop) {
    this.name = prop.getProperty("name");
    this.description = prop.getProperty("description");
    this.main = prop.getProperty("main");
    this.topics = prop.getProperty("topic").replace(" ", "").split(",");
    this.qos = Integer.parseInt(prop.getProperty("qos"));
    this.broker = prop.getProperty("broker");
    this.username = prop.getProperty("username", "");
    this.password = prop.getProperty("password", "").toCharArray();
    this.clientId = prop.getProperty("clientId");
    this.updateWait = Integer.parseInt(prop.getProperty("updateWait"));
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
  
  public String[] getTopics() {
    return topics;
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

  public int getUpdateWait() {
    return updateWait;
  }

}
