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

import me.kevinthorne.MQTTBlocks.blocks.BlockConfigurationFile;
import me.kevinthorne.MQTTBlocks.blocks.BlockLoader;
import me.kevinthorne.MQTTBlocks.blocks.MQTTBlock;

public class BlockManager extends Thread {

  private static Logger logger = Logger.getLogger(BlockManager.class.getName());

  public static final File blockLocation = new File("blocks/");

  private Map<String, MQTTBlock> blocks = new HashMap<>();
  // private Map<String, Future> enabledComponents = new HashMap<>();

  public BlockManager() {
    logger.info("Building runtime...");
    
    logger.setUseParentHandlers(false);

    Formatter formatter = new BlockLogFormatter();
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

    loadCoreBlocks();
    logger.info(blocks.size() + " core component(s) successfully added");

    enableBlocks();
    logger.info(blocks.size() + " core component(s) successfully enabled");

    Runtime.getRuntime().addShutdownHook(this);
    
    logger.info("Block Manager started successfully");
  }

  /**
   * OnShutdown event
   */
  public void run() {
    logger.info("Shutdown initiated");
    disableBlocks();
    removeBlocks();
    logger.info("Halted.");
  }

  public void enableBlocks() {
    for (String blockName : blocks.keySet()) {
      enableBlock(blockName);
    }
  }

  public void disableBlocks() {
    for (String blockName : blocks.keySet()) {
      disableBlock(blockName);
    }
  }

  public void removeBlocks() {
    for (String blockName : blocks.keySet()) {
      removeBlock(blockName);
    }
  }

  public void addBlock(BlockConfigurationFile config, MQTTBlock block) {
    if (blocks.get(config) == null) {
      block.init(this, config);
      blocks.put(config.getName(), block);
    } else {
      logger.severe("Could not add " + config.getName() + ": Block already exists!");
    }
  }

  public void enableBlock(String name) {
    try {
      blocks.get(name).start();
    } catch (Exception e) {
      logger.severe("Block: " + name + " could not be enabled, removing.");
      e.printStackTrace();
    }
  }

  public void disableBlock(String name) {
    try {
      blocks.get(name).interrupt();
    } catch (Exception e) {
      logger.severe("Block: " + name + " could not be disabled, removing.");
      e.printStackTrace();
    }
  }

  public void removeBlock(String name) {
    try {
      blocks.remove(name);
    } catch (Exception ignored) {

    }
  }

  /**
   * Loads all jar files in the "components" folder, instantiates class that is specified in
   * config.properties and that extends MQTTComponent
   * 
   */
  public void loadCoreBlocks() {
    if (!blockLocation.exists())
      blockLocation.mkdirs();

    addBlock(new BlockConfigurationFile("ComponentLoader", null, null, null, 2, null, null,
        null, null, 10), new BlockLoader());
  }


  public static void main(String[] args) {
    new BlockManager();
  }


  public static void logError(MQTTBlock source, String log) {
    logger.severe("[" + source.getBlockName() + "] - " + log);
  }

  public static void logInfo(MQTTBlock source, String log) {
    logger.info("[" + source.getBlockName() + "] - " + log);
  }

  public static void logWarn(MQTTBlock source, String log) {
    logger.warning("[" + source.getBlockName() + "] - " + log);
  }

  public static void logConfig(MQTTBlock source, String log) {
    logger.config("[" + source.getBlockName() + "] - " + log);
  }

  public Logger getLogger() {
    return logger;
  }

  public Map<String, MQTTBlock> getBlocks() {
    return blocks;
  }

  public static class BlockLogFormatter extends Formatter {
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
