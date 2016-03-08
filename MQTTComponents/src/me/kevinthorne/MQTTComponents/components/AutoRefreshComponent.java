package me.kevinthorne.MQTTComponents.components;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.jar.JarFile;

import me.kevinthorne.MQTTComponents.ComponentManager;

public class AutoRefreshComponent extends MQTTComponent {

  private Date started;

  private boolean running = true;

  @Override
  public void init(ComponentManager parent, ComponentConfigurationFile config) {
    this.parent = parent;
    this.config = config;

    this.name = config.getName();

  }

  @Override
  public void onEnable() throws Exception {
    started = new Date();
    startThread();
    ComponentManager.logInfo(this, "Enabled Successfully");
  }

  @Override
  public void onDisable() throws Exception {
    running = false;
    stopThread(false);
  }

  @Override
  public void run() {
    while (running) {
      ComponentManager.logInfo(this, "Checking for new components...");

      File[] jars = ComponentManager.componentLocation.listFiles();
      
      for (File jar : jars) {
        //System.out.println("[started: " + started.toString()+ "] - " + jar.getName() + " - ["+new Date(jar.lastModified()).toString()+"]");
        //System.out.println(started.before(new Date(jar.lastModified())));
        if (started.after(new Date(jar.lastModified()))) {
          System.out.println("Found jar file after start date, adding");
          try {
            JarFile jarFile = new JarFile(jar);
            ComponentConfigurationFile config = new ComponentConfigurationFile(
                jarFile.getInputStream(jarFile.getJarEntry("config.properties")));

            System.out.println("Loaded config");
            
            URL[] urls = {new URL("jar:file:" + jar + "!/")};
            URLClassLoader cl = URLClassLoader.newInstance(urls);

            Class<?> jarClass;
            try {
              jarClass = Class.forName(config.getMain(), true, cl);
            } catch (ClassNotFoundException ex) {
              ComponentManager.logError(this, "Couldn't find main for " + jar.getName());
              continue;
            }
            System.out.println("Finding component subclass");
            Class<? extends MQTTComponent> componentClass;
            try {
              componentClass = jarClass.asSubclass(MQTTComponent.class);
            } catch (ClassCastException ex) {
              ComponentManager.logError(this,
                  "Couldn't find Component subclass for " + jar.getName());
              continue;
            }
            System.out.println("Instantiating and registering");
            try {
              MQTTComponent comp = componentClass.newInstance();
              if (getParent().getComponents().containsKey(config.getName())) {
                getParent().removeComponent(config.getName());
              }
              getParent().addComponent(config, comp);
              ComponentManager.logInfo(this, "Added " + config.getName());
            } catch (InstantiationException | IllegalAccessException e1) {
              ComponentManager.logError(this, "Couldn't instantiate " + jar.getName());
              e1.printStackTrace();
              continue;
            }
            jar.setLastModified(new Date().getTime());
            System.out.println("File completed");
            jarFile.close();
          } catch (IOException e) {
            logger.severe("Couldn't find config for " + jar.getName());
            continue;
          }
        }
      }

      ComponentManager.logInfo(this, "Routine complete.");
      try {
        Thread.sleep(10000);
      } catch (InterruptedException ignored) {
      }
    }

  }

  @Override
  public void onMessageReceived(String topic, Object mqttMessage, String message, int qos,
      boolean isDuplicate, boolean isRetained) {
    // TODO Auto-generated method stub

  }

}
