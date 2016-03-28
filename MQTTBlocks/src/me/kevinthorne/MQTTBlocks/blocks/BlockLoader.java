package me.kevinthorne.MQTTBlocks.blocks;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.jar.JarFile;

import me.kevinthorne.MQTTBlocks.BlockManager;

public class BlockLoader extends MQTTBlock {

  private Date started;

  private boolean firstLoop = true;

  @Override
  public void onEnable() {
    started = new Date();
    BlockManager.logInfo(this, "Block Loader Daemon Started");
  }

  @Override
  public void onDisable() {
    BlockManager.logInfo(this, "Block Loader Daemon Stopped");
  }

  public void run() {
    this.name = getBlockConfig().getName();
    try {
      onEnable();
      setRunning(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
    while (isRunning()) {
      update();
      try {
        Thread.sleep(getSleepTime() * 100);
      } catch (InterruptedException ignored) {
      }
    }
    try {
      onDisable();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void update() {
    // ComponentManager.logInfo(this, "Checking for new components...");

    File[] jars = BlockManager.blockLocation.listFiles();

    for (File jar : jars) {
      // System.out.println("[started: " + started.toString()+ "] - " + jar.getName() + " - ["+new
      // Date(jar.lastModified()).toString()+"]");
      // System.out.println(started.before(new Date(jar.lastModified())));
      if (started.before(new Date(jar.lastModified())) || firstLoop) {
        firstLoop = false;
        // System.out.println("Found jar file after start date, adding");
        try {
          JarFile jarFile = new JarFile(jar);
          BlockConfigurationFile config;
          try {
             config = new BlockConfigurationFile(
                jarFile.getInputStream(jarFile.getJarEntry("config.properties")));
          } catch (NullPointerException ne) {
            logError("Couldn't find config.properties for \"" + jar.getName() + "\"");
            continue;
          }

          // System.out.println("Loaded config");

          URL[] urls = {new URL("jar:file:" + jar + "!/")};
          URLClassLoader cl = URLClassLoader.newInstance(urls);

          Class<?> jarClass;
          try {
            jarClass = Class.forName(config.getMain(), true, cl);
          } catch (ClassNotFoundException ex) {
            logError("Couldn't find main for \"" + jar.getName() + "\" reload");
            continue;
          } catch (NullPointerException ne) {
            logError("Couldn't find main for \"" + jar.getName() + "\" reload");
            logError("\tconfig Main Method: " + config.getMain());
            logError("\tClass Loader: " + cl);
            continue;
          }
          // System.out.println("Finding component subclass");
          Class<? extends MQTTBlock> componentClass;
          try {
            componentClass = jarClass.asSubclass(MQTTBlock.class);
          } catch (ClassCastException ex) {
            logError("Couldn't find Component subclass for " + jar.getName());
            continue;
          }
          // System.out.println("Instantiating and registering");
          try {
            if (getParent().getBlocks().containsKey(config.getName())) {
              // System.out.println("Removing Old Component...");
              getParent().disableBlock(config.getName());
              getParent().removeBlock(config.getName());
            }
            // System.out.print("Instantiating...");
            MQTTBlock comp = componentClass.newInstance();
            // System.out.println(" Done");
            // System.out.println("Registering...");
            getParent().addBlock(config, comp);
            getParent().enableBlock(config.getName());
            logInfo("Enabled new block \"" + config.getName() + "\"");
          } catch (InstantiationException | IllegalAccessException e1) {
            logError("Couldn't instantiate \"" + jar.getName() + "\"");
            e1.printStackTrace();
            continue;
          }
          jar.setLastModified(started.getTime());
          // System.out.println("File completed");
          jarFile.close();
        } catch (IOException e) {
          logger.severe("Couldn't find config for " + jar.getName());
          continue;
        }
      } else {
        continue;
      }
    }
  }


  @Override
  public boolean onMessageReceived(String topic, Object mqttMessage, String message, int qos,
      boolean isDuplicate, boolean isRetained, boolean fromHome) {
    return true;
  }

}
