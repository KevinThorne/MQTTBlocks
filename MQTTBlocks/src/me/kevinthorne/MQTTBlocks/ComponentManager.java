package me.kevinthorne.MQTTBlocks;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import me.kevinthorne.MQTTBlocks.components.ComponentConfigurationFile;
import me.kevinthorne.MQTTBlocks.components.ComponentLoader;
import me.kevinthorne.MQTTBlocks.components.MQTTComponent;

public class ComponentManager extends Thread {

  private static Logger logger = Logger.getLogger(ComponentManager.class.getName());

  public static final File componentLocation = new File("components/");

  private Map<String, MQTTComponent> components = new HashMap<>();
  // private Map<String, Future> enabledComponents = new HashMap<>();

  public ComponentManager() {
    logger.info("Building runtime...");
    
    logger.setUseParentHandlers(false);

    Formatter formatter = new ComponentLogFormatter();
    ConsoleHandler handler = new ConsoleHandler();
    try {
      Handler fileHandler = new FileHandler("MQTTBlocks.log");
      handler.setFormatter(formatter);
      logger.addHandler(fileHandler);
    } catch (SecurityException | IOException e) {
      e.printStackTrace();
    }
    handler.setFormatter(formatter);
    logger.addHandler(handler);
    logger.info("Logger setup successful");

    loadComponents();
    logger.info(components.size() + " core component(s) successfully added");

    enableComponents();
    logger.info(components.size() + " core component(s) successfully enabled");

    Runtime.getRuntime().addShutdownHook(this);
    
    logger.info("Component Manager started successfully");
  }

  /**
   * OnShutdown event
   */
  public void run() {
    logger.info("Shutdown initiated");
    disableComponents();
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
      components.get(name).start();
    } catch (Exception e) {
      logger.severe("Component: " + name + " could not be enabled, removing.");
      e.printStackTrace();
    }
  }

  public void disableComponent(String name) {
    try {
      components.get(name).interrupt();
    } catch (Exception e) {
      logger.severe("Component: " + name + " could not be disabled, removing.");
      e.printStackTrace();
    }
  }

  public void removeComponent(String name) {
    try {
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

    addComponent(new ComponentConfigurationFile("ComponentLoader", null, null, null, 2, null, null,
        null, null, 10), new ComponentLoader());
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

  public static void logConfig(MQTTComponent source, String log) {
    logger.config("[" + source.getComponentName() + "] - " + log);
  }

  public Logger getLogger() {
    return logger;
  }

  public Map<String, MQTTComponent> getComponents() {
    return components;
  }

  public static class ComponentLogFormatter extends Formatter {
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
