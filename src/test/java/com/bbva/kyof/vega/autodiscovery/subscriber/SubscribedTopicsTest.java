package com.bbva.kyof.vega.autodiscovery.subscriber;

import com.bbva.kyof.vega.TestConstants;
import com.bbva.kyof.vega.autodiscovery.model.AutoDiscTopicInfo;
import com.bbva.kyof.vega.autodiscovery.model.AutoDiscTopicSocketInfo;
import com.bbva.kyof.vega.autodiscovery.model.AutoDiscTransportType;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

/**
 * Created by cnebrera on 04/08/16.
 */
public class SubscribedTopicsTest {
    @Test
    public void testAddRemoveListeners() {
        final SubscribedTopics subTopics = new SubscribedTopics();
        final ListenerImpl listener1 = new ListenerImpl();
        final ListenerImpl listener2 = new ListenerImpl();

        Assert.assertTrue(subTopics.addListener(AutoDiscTransportType.PUB_IPC, "topic", listener1));
        Assert.assertTrue(subTopics.addListener(AutoDiscTransportType.PUB_MUL, "topic", listener1));
        Assert.assertTrue(subTopics.addListener(AutoDiscTransportType.PUB_UNI, "topic", listener1));
        Assert.assertTrue(subTopics.addListener(AutoDiscTransportType.SUB_IPC, "topic", listener1));
        Assert.assertTrue(subTopics.addListener(AutoDiscTransportType.SUB_MUL, "topic", listener1));
        Assert.assertTrue(subTopics.addListener(AutoDiscTransportType.SUB_UNI, "topic", listener1));

        // Try to add again on the same topic and transport, should be false since the listener is already there
        Assert.assertFalse(subTopics.addListener(AutoDiscTransportType.PUB_IPC, "topic", listener1));

        // But if we try again with a different listener should work
        Assert.assertTrue(subTopics.addListener(AutoDiscTransportType.PUB_IPC, "topic", listener2));

        // Try on a different topic, should be true
        Assert.assertTrue(subTopics.addListener(AutoDiscTransportType.PUB_IPC, "topic2", listener1));

        // Now try to remove on non existing topic or transport
        Assert.assertFalse(subTopics.removeListener(AutoDiscTransportType.SUB_UNI, "topic2", listener1));
        Assert.assertFalse(subTopics.removeListener(AutoDiscTransportType.PUB_IPC, "topic3", listener1));

        // Now try with non existing listener
        Assert.assertFalse(subTopics.removeListener(AutoDiscTransportType.SUB_UNI, "topic", listener2));

        // Now try with existing listener
        Assert.assertTrue(subTopics.removeListener(AutoDiscTransportType.PUB_IPC, "topic", listener2));
        Assert.assertFalse(subTopics.removeListener(AutoDiscTransportType.PUB_IPC, "topic", listener2));
        Assert.assertTrue(subTopics.removeListener(AutoDiscTransportType.PUB_IPC, "topic", listener1));
        Assert.assertFalse(subTopics.removeListener(AutoDiscTransportType.PUB_IPC, "topic", listener1));

        // Clear
        subTopics.clear();

        // Try again
        Assert.assertFalse(subTopics.removeListener(AutoDiscTransportType.PUB_MUL, "topic", listener1));
        Assert.assertFalse(subTopics.removeListener(AutoDiscTransportType.PUB_UNI, "topic", listener1));
        Assert.assertFalse(subTopics.removeListener(AutoDiscTransportType.SUB_IPC, "topic", listener1));
        Assert.assertFalse(subTopics.removeListener(AutoDiscTransportType.SUB_MUL, "topic", listener1));
        Assert.assertFalse(subTopics.removeListener(AutoDiscTransportType.SUB_UNI, "topic", listener1));
    }

    @Test
    public void testAdverts() {
        final UUID instanceId = UUID.randomUUID();
        final SubscribedTopics subTopics = new SubscribedTopics();
        final ListenerImpl listener1 = new ListenerImpl();
        final ListenerImpl listener2 = new ListenerImpl();

        Assert.assertTrue(subTopics.addListener(AutoDiscTransportType.PUB_IPC, "topic", listener1));
        Assert.assertTrue(subTopics.addListener(AutoDiscTransportType.PUB_IPC, "topic", listener2));
        Assert.assertTrue(subTopics.addListener(AutoDiscTransportType.PUB_UNI, "topic2", listener1));
        Assert.assertTrue(subTopics.addListener(AutoDiscTransportType.PUB_IPC, "topic2", listener1));
        Assert.assertTrue(subTopics.addListener(AutoDiscTransportType.SUB_MUL, "topic2", listener2));

        // Non registered due to transport or topic, nothing should happen
        subTopics.onNewTopicInfo(new AutoDiscTopicInfo(instanceId, AutoDiscTransportType.SUB_MUL, UUID.randomUUID(), "topic"));
        subTopics.onNewTopicInfo(new AutoDiscTopicInfo(instanceId, AutoDiscTransportType.PUB_IPC, UUID.randomUUID(), "topic55"));
        Assert.assertEquals(0, listener1.numNewTopicCalls);
        Assert.assertEquals(0, listener2.numNewTopicCalls);

        // Now try on topic with pub ipc, should update both listeners
        subTopics.onNewTopicInfo(new AutoDiscTopicInfo(instanceId, AutoDiscTransportType.PUB_IPC, UUID.randomUUID(), "topic"));
        Assert.assertEquals(1, listener1.numNewTopicCalls);
        Assert.assertEquals(1, listener2.numNewTopicCalls);

        // Now try on topic2 with sub mul, should update only listener 2
        subTopics.onNewTopicInfo(new AutoDiscTopicInfo(instanceId, AutoDiscTransportType.SUB_MUL, UUID.randomUUID(), "topic2"));
        Assert.assertEquals(1, listener1.numNewTopicCalls);
        Assert.assertEquals(2, listener2.numNewTopicCalls);

        // ---------------------------- Repeat for time outs ----------------------------------------
        // Non registered due to transport or topic, nothing should happen
        subTopics.onTimedOutTopicInfo(new AutoDiscTopicInfo(instanceId, AutoDiscTransportType.SUB_MUL, UUID.randomUUID(), "topic"));
        subTopics.onTimedOutTopicInfo(new AutoDiscTopicInfo(instanceId, AutoDiscTransportType.PUB_IPC, UUID.randomUUID(), "topic55"));
        Assert.assertEquals(0, listener1.numTimeoutTopicCalls);
        Assert.assertEquals(0, listener2.numTimeoutTopicCalls);

        // Now try on topic with pub ipc, should update both listeners
        subTopics.onTimedOutTopicInfo(new AutoDiscTopicInfo(instanceId, AutoDiscTransportType.PUB_IPC, UUID.randomUUID(), "topic"));
        Assert.assertEquals(1, listener1.numTimeoutTopicCalls);
        Assert.assertEquals(1, listener2.numTimeoutTopicCalls);

        // Now try on topic2 with sub mul, should update only listener 2
        subTopics.onTimedOutTopicInfo(new AutoDiscTopicInfo(instanceId, AutoDiscTransportType.SUB_MUL, UUID.randomUUID(), "topic2"));
        Assert.assertEquals(1, listener1.numTimeoutTopicCalls);
        Assert.assertEquals(2, listener2.numTimeoutTopicCalls);

        // ---------------------------- Repeat for topic sockets ----------------------------------------
        subTopics.onNewTopicSocketInfo(new AutoDiscTopicSocketInfo(instanceId, AutoDiscTransportType.SUB_MUL, UUID.randomUUID(), "topic", UUID.randomUUID(), 1, 2, 3, TestConstants.EMPTY_HOSTNAME));
        subTopics.onNewTopicSocketInfo(new AutoDiscTopicSocketInfo(instanceId, AutoDiscTransportType.PUB_IPC, UUID.randomUUID(), "topic55", UUID.randomUUID(), 1, 2, 3, TestConstants.EMPTY_HOSTNAME));
        Assert.assertEquals(0, listener1.numNewTopicSocketCalls);
        Assert.assertEquals(0, listener2.numNewTopicSocketCalls);

        // Now try on topic with pub ipc, should update both listeners
        subTopics.onNewTopicSocketInfo(new AutoDiscTopicSocketInfo(instanceId, AutoDiscTransportType.PUB_IPC, UUID.randomUUID(), "topic", UUID.randomUUID(), 1, 2, 3, TestConstants.EMPTY_HOSTNAME));
        Assert.assertEquals(1, listener1.numNewTopicSocketCalls);
        Assert.assertEquals(1, listener2.numNewTopicSocketCalls);

        // Now try on topic2 with sub mul, should update only listener 2
        subTopics.onNewTopicSocketInfo(new AutoDiscTopicSocketInfo(instanceId, AutoDiscTransportType.SUB_MUL, UUID.randomUUID(), "topic2", UUID.randomUUID(), 1, 2, 3, TestConstants.EMPTY_HOSTNAME));
        Assert.assertEquals(1, listener1.numNewTopicSocketCalls);
        Assert.assertEquals(2, listener2.numNewTopicSocketCalls);

        // ---------------------------- Repeat for topic sockets timeouts ----------------------------------------
        subTopics.onTimedOutTopicSocketInfo(new AutoDiscTopicSocketInfo(instanceId, AutoDiscTransportType.SUB_MUL, UUID.randomUUID(), "topic", UUID.randomUUID(), 1, 2, 3, TestConstants.EMPTY_HOSTNAME));
        subTopics.onTimedOutTopicSocketInfo(new AutoDiscTopicSocketInfo(instanceId, AutoDiscTransportType.PUB_IPC, UUID.randomUUID(), "topic55", UUID.randomUUID(), 1, 2,
                3, TestConstants.EMPTY_HOSTNAME));
        Assert.assertEquals(0, listener1.numTimeoutTopicSocketCalls);
        Assert.assertEquals(0, listener2.numTimeoutTopicSocketCalls);

        // Now try on topic with pub ipc, should update both listeners
        subTopics.onTimedOutTopicSocketInfo(new AutoDiscTopicSocketInfo(instanceId, AutoDiscTransportType.PUB_IPC, UUID.randomUUID(), "topic", UUID.randomUUID(), 1, 2, 3, TestConstants.EMPTY_HOSTNAME));
        Assert.assertEquals(1, listener1.numTimeoutTopicSocketCalls);
        Assert.assertEquals(1, listener2.numTimeoutTopicSocketCalls);

        // Now try on topic2 with sub mul, should update only listener 2
        subTopics.onTimedOutTopicSocketInfo(new AutoDiscTopicSocketInfo(instanceId, AutoDiscTransportType.SUB_MUL, UUID.randomUUID(), "topic2", UUID.randomUUID(), 1, 2, 3, TestConstants.EMPTY_HOSTNAME));
        Assert.assertEquals(1, listener1.numTimeoutTopicSocketCalls);
        Assert.assertEquals(2, listener2.numTimeoutTopicSocketCalls);
    }

    public static class ListenerImpl implements IAutodiscTopicSubListener {
        public int numNewTopicCalls = 0;
        public int numNewTopicSocketCalls = 0;
        public int numTimeoutTopicCalls = 0;
        public int numTimeoutTopicSocketCalls = 0;


        @Override
        public void onNewAutoDiscTopicInfo(AutoDiscTopicInfo info) {
            numNewTopicCalls++;
        }

        @Override
        public void onTimedOutAutoDiscTopicInfo(AutoDiscTopicInfo info) {
            numTimeoutTopicCalls++;
        }

        @Override
        public void onNewAutoDiscTopicSocketInfo(AutoDiscTopicSocketInfo info) {
            numNewTopicSocketCalls++;
        }

        @Override
        public void onTimedOutAutoDiscTopicSocketInfo(AutoDiscTopicSocketInfo info) {
            numTimeoutTopicSocketCalls++;
        }
    }
}