package test.me.kevinthorne.MQTTBlocks;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import me.kevinthorne.MQTTBlocks.BlockManager;
import me.kevinthorne.MQTTBlocks.blocks.BlockConfigurationFile;
import me.kevinthorne.MQTTBlocks.blocks.MQTTBlock;

public class AddRemoveCoreBlockTest {

  private static BlockManager blockManager;

  private static MQTTBlock testBlock;
  private static BlockConfigurationFile testBlockConfig;

  private static class TestBlock extends MQTTBlock {

    @Override
    public void onEnable() {
      // TODO Auto-generated method stub

    }

    @Override
    public void onDisable() {
      // TODO Auto-generated method stub

    }

    @Override
    public void update() {
      // TODO Auto-generated method stub

    }

    @Override
    public boolean onMessageReceived(String topic, Object mqttMessage, String message, int qos,
        boolean isDuplicate, boolean isRetained, boolean fromHome) {
      // TODO Auto-generated method stub
      return false;
    }


  }

  @BeforeClass
  public static void setUp() throws Exception {

    blockManager = new BlockManager();
    testBlock = new TestBlock();
    testBlockConfig =
        new BlockConfigurationFile("TestBlock", null, null, null, 2, null, null, null, null, 10);

  }

  @Test
  public void testAdd() {

    blockManager.addBlock(testBlockConfig, testBlock);

    Assert.assertTrue("Block \"TestBlock\" wasn't added",
        blockManager.getBlocks().containsKey("TestBlock"));

  }

  @Test
  public void testRemove() {

    blockManager.removeBlock("TestBlock");

    Assert.assertTrue("Block \"TestBlock\" wasn't removed",
        !blockManager.getBlocks().containsKey("TestBlock"));

  }

  @AfterClass
  public static void tearDown() {
    blockManager.shutdown();
  }

}
