package me.kevinthorne.MQTTComponents;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import me.kevinthorne.MQTTComponents.components.AutoRefreshComponent;
import me.kevinthorne.MQTTComponents.components.ComponentConfigurationFile;
import me.kevinthorne.MQTTComponents.components.MQTTComponent;

public class ComponentManager extends Thread {

  private static Logger logger = Logger.getLogger(ComponentManager.class.getName());

  public static final File componentLocation = new File("components/");

  private Map<String, MQTTComponent> components = new HashMap<>();

  public ComponentManager() {
    logger.setUseParentHandlers(false);

    MQTTManagerFormatter formatter = new MQTTManagerFormatter();
    ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter(formatter);
    logger.addHandler(handler);
    logger.info("Logger setup successful");

    loadComponents();
    logger.info(components.size() + " components successfully added");

    enableComponents();
    logger.info(components.size() + " components successfully enabled");


    Runtime.getRuntime().addShutdownHook(this);
  }

  /**
   * On shutdown event
   */
  public void run() {
    logger.info("Shutdown initiated");
    removeComponents();
    logger.info("Halted.");
  }

  public void enableComponents() {
    for (String compName : components.keySet()) {
      enableComponent(compName);
    }
  }

  public void disableComponents() {
    for (String compName : components.keySet()) {
      disableComponent(compName);
    }
  }

  public void removeComponents() {
    for (String compName : components.keySet()) {
      removeComponent(compName);
    }
  }

  public void addComponent(ComponentConfigurationFile config, MQTTComponent comp) {
    if (components.get(config) == null) {
      comp.init(this, config);
      components.put(config.getName(), comp);
    } else {
      logger.severe("Could not add " + config.getName() + ": Component already exists!");
    }
  }

  public void enableComponent(String name) {
    try {
      components.get(name).onEnable();
    } catch (Exception e) {
      logger.severe("Component: " + name + " could not be enabled, removing.");
      e.printStackTrace();
    }
  }

  public void disableComponent(String name) {
    try {
      components.get(name).onDisable();
    } catch (Exception e) {
      logger.severe("Component: " + name + " could not be disabled, removing.");
      e.printStackTrace();
    }
  }

  public void removeComponent(String name) {
    try {
      components.get(name).destroy();
      components.remove(name);
    } catch (Exception ignored) {

    }
  }

  /**
   * Loads all jar files in the "components" folder, instantiates class that is specified in
   * config.properties and that extends MQTTComponent
   * 
   */
  public void loadComponents() {
    if (!componentLocation.exists())
      componentLocation.mkdirs();
    
    addComponent(new ComponentConfigurationFile("AutoRefreshComponent", null, null, null, 2, null, null), 
        new AutoRefreshComponent());
  }


  public static void main(String[] args) {
    new ComponentManager();
  }


  public static void logError(MQTTComponent source, String log) {
    logger.severe("[" + source.getComponentName() + "] - " + log);
  }

  public static void logInfo(MQTTComponent source, String log) {
    logger.info("[" + source.getComponentName() + "] - " + log);
  }

  public static void logWarn(MQTTComponent source, String log) {
    logger.warning("[" + source.getComponentName() + "] - " + log);
  }

  public Logger getLogger() {
    return logger;
  }

  public Map<String, MQTTComponent> getComponents() {
    return components;
  }

  public static class MQTTManagerFormatter extends Formatter {
    //
    // Create a DateFormat to format the logger timestamp.
    //
    private static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

    public String format(LogRecord record) {
      StringBuilder builder = new StringBuilder(1000);
      builder.append(df.format(new Date(record.getMillis()))).append(" - ");
      builder.append("[").append(record.getLevel()).append("] - ");
      builder.append(formatMessage(record));
      builder.append("\n");
      return builder.toString();
    }

    public String getHead(Handler h) {
      return super.getHead(h);
    }

    public String getTail(Handler h) {
      return super.getTail(h);
    }
  }

}
