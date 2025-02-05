package com.bbva.kyof.vega.protocol.subscriber;

import com.bbva.kyof.vega.TestConstants;
import com.bbva.kyof.vega.autodiscovery.model.AutoDiscTopicInfo;
import com.bbva.kyof.vega.autodiscovery.model.AutoDiscTopicSocketInfo;
import com.bbva.kyof.vega.autodiscovery.model.AutoDiscTransportType;
import com.bbva.kyof.vega.config.general.ConfigReader;
import com.bbva.kyof.vega.config.general.ConfigReaderTest;
import com.bbva.kyof.vega.config.general.TopicTemplateConfig;
import com.bbva.kyof.vega.config.general.TransportMediaType;
import com.bbva.kyof.vega.msg.*;
import com.bbva.kyof.vega.protocol.AutoDiscManagerMock;
import com.bbva.kyof.vega.protocol.common.VegaContext;
import com.bbva.kyof.vega.protocol.control.ISecurityRequesterNotifier;
import com.bbva.kyof.vega.protocol.publisher.AeronPublisher;
import com.bbva.kyof.vega.protocol.publisher.AeronPublisherParams;
import com.bbva.kyof.vega.util.net.InetUtil;
import com.bbva.kyof.vega.util.net.SubnetAddress;
import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import org.agrona.CloseHelper;
import org.agrona.concurrent.UnsafeBuffer;
import org.easymock.EasyMock;
import org.junit.*;

import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by cnebrera on 11/08/16.
 */
public class SubscribersManagerUnicastTest implements ITopicSubListener {
    private static final String validConfigFile = Objects.requireNonNull(ConfigReaderTest.class.getClassLoader().getResource("config/subscribersManagerTestConfig.xml")).getPath();

    private static MediaDriver MEDIA_DRIVER;
    private static Aeron AERON;
    private static SubnetAddress SUBNET_ADDRESS;
    private static VegaContext VEGA_CONTEXT;
    private static AutoDiscManagerMock AUTO_DISC_MANAGER_MOCK;
    private static SubscribersPollersManager POLLERS_MANAGER;
    private static final ReceiverListener POLLER_LISTENER = new ReceiverListener();
    private static final TopicSubAndTopicPubIdRelations RELATIONS = new TopicSubAndTopicPubIdRelations();
    private static TopicTemplateConfig TEMPLATE_UCAST;

    private SubscribersManagerUnicast subscriberManager;

    @BeforeClass
    public static void beforeClass() throws Exception {
        MEDIA_DRIVER = MediaDriver.launchEmbedded();

        final Aeron.Context ctx1 = new Aeron.Context();
        ctx1.aeronDirectoryName(MEDIA_DRIVER.aeronDirectoryName());

        AERON = Aeron.connect(ctx1);

        SUBNET_ADDRESS = InetUtil.getDefaultSubnet();

        Assert.assertNotNull(SUBNET_ADDRESS);

        VEGA_CONTEXT = new VegaContext(AERON, ConfigReader.readConfiguration(validConfigFile));

        // Mock auto-discovery manager calls
        AUTO_DISC_MANAGER_MOCK = new AutoDiscManagerMock();
        VEGA_CONTEXT.setAutodiscoveryManager(AUTO_DISC_MANAGER_MOCK.getMock());

        // Create the pollers manager
        POLLERS_MANAGER = new SubscribersPollersManager(VEGA_CONTEXT, POLLER_LISTENER);

        TEMPLATE_UCAST = TopicTemplateConfig.builder().
                name("template1").
                transportType(TransportMediaType.UNICAST).
                numStreamsPerPort(2).
                minPort(28401).
                maxPort(28402).
                rcvPoller("poller1").
                subnetAddress(SUBNET_ADDRESS).build();

        // Give it time to start
        Thread.sleep(1000);
    }

    @AfterClass
    public static void afterClass() {
        AERON.close();
        CloseHelper.quietClose(MEDIA_DRIVER);
    }

    @Before
    public void before() {
        final ISecurityRequesterNotifier securityRequesterNotifier = EasyMock.createNiceMock(ISecurityRequesterNotifier.class);
        EasyMock.replay(securityRequesterNotifier);
        subscriberManager = new SubscribersManagerUnicast(VEGA_CONTEXT, POLLERS_MANAGER, RELATIONS, securityRequesterNotifier);
    }

    @After
    public void after() {
        subscriberManager.close();
    }

    @Test
    public void testSubscribeUnsubscribe() throws Exception {
        final UUID instanceId = UUID.randomUUID();

        // Subscribe to 4 TOPICS, since there are ony 2 ports some of the aeron subscribers will be repeated
        subscriberManager.subscribeToTopic("utopic1", TEMPLATE_UCAST, null, this);
        subscriberManager.subscribeToTopic("utopic2", TEMPLATE_UCAST, null, this);
        subscriberManager.subscribeToTopic("utopic3", TEMPLATE_UCAST, null, this);
        subscriberManager.subscribeToTopic("utopic4", TEMPLATE_UCAST, null, this);

        // These are the topic info for each created publisher
        final AutoDiscTopicInfo topicInfo1 = new AutoDiscTopicInfo(instanceId, AutoDiscTransportType.SUB_UNI, subscriberManager.getTopicSubscriberForTopicName("utopic1").getUniqueId(), "utopic1");
        final AutoDiscTopicInfo topicInfo2 = new AutoDiscTopicInfo(instanceId, AutoDiscTransportType.SUB_UNI, subscriberManager.getTopicSubscriberForTopicName("utopic2").getUniqueId(), "utopic2");
        final AutoDiscTopicInfo topicInfo3 = new AutoDiscTopicInfo(instanceId, AutoDiscTransportType.SUB_UNI, subscriberManager.getTopicSubscriberForTopicName("utopic3").getUniqueId(), "utopic3");
        final AutoDiscTopicInfo topicInfo4 = new AutoDiscTopicInfo(instanceId, AutoDiscTransportType.SUB_UNI, subscriberManager.getTopicSubscriberForTopicName("utopic4").getUniqueId(), "utopic4");

        // 4 new auto-discovery topic info should have been registered in auto-discovery
        assertTrue(AUTO_DISC_MANAGER_MOCK.getRegTopicInfos().contains(topicInfo1));
        assertTrue(AUTO_DISC_MANAGER_MOCK.getRegTopicInfos().contains(topicInfo2));
        assertTrue(AUTO_DISC_MANAGER_MOCK.getRegTopicInfos().contains(topicInfo3));
        assertTrue(AUTO_DISC_MANAGER_MOCK.getRegTopicInfos().contains(topicInfo4));

        // There should be 4 topic sockets, one per topic publisher but only 2 actual publishers are created
        Assert.assertEquals(4, AUTO_DISC_MANAGER_MOCK.getRegTopicSocketInfos().size());

        // Review them
        AUTO_DISC_MANAGER_MOCK.getRegTopicSocketInfos().forEach((topicSocket) ->
        {
            assertTrue(topicSocket.getPort() == 28401 || topicSocket.getPort() == 28402);
            assertSame(topicSocket.getTransportType(), AutoDiscTransportType.SUB_UNI);
            assertTrue(topicSocket.getStreamId() == 2 || topicSocket.getStreamId() == 3);
            assertTrue(topicSocket.getTopicName().startsWith("utopic"));
        });

        // Now un-subscribe one by one...
        subscriberManager.unsubscribeFromTopic("utopic1");
        assertEquals(3, AUTO_DISC_MANAGER_MOCK.getRegTopicSocketInfos().size());
        Assert.assertFalse(AUTO_DISC_MANAGER_MOCK.getRegTopicInfos().contains(topicInfo1));

        subscriberManager.unsubscribeFromTopic("utopic2");
        assertEquals(2, AUTO_DISC_MANAGER_MOCK.getRegTopicSocketInfos().size());
        Assert.assertFalse(AUTO_DISC_MANAGER_MOCK.getRegTopicInfos().contains(topicInfo2));

        subscriberManager.unsubscribeFromTopic("utopic3");
        assertEquals(1, AUTO_DISC_MANAGER_MOCK.getRegTopicSocketInfos().size());
        Assert.assertFalse(AUTO_DISC_MANAGER_MOCK.getRegTopicInfos().contains(topicInfo3));

        subscriberManager.unsubscribeFromTopic("utopic4");
        assertEquals(0, AUTO_DISC_MANAGER_MOCK.getRegTopicSocketInfos().size());
        Assert.assertFalse(AUTO_DISC_MANAGER_MOCK.getRegTopicInfos().contains(topicInfo4));
    }

    @Test
    public void testReceive() throws Exception {
        // Subscribe to 4 TOPICS, since there are ony 2 ports some of the aeron subscribers will be repeated
        subscriberManager.subscribeToTopic("utopic1", TEMPLATE_UCAST, null, this);
        subscriberManager.subscribeToTopic("utopic2", TEMPLATE_UCAST, null, this);
        subscriberManager.subscribeToTopic("utopic3", TEMPLATE_UCAST, null, this);
        subscriberManager.subscribeToTopic("utopic4", TEMPLATE_UCAST, null, this);

        final HashMap<String, AeronPublisher> publishersByTopic = new LinkedHashMap<>();

        // Now create a publisher for each registered topic socket
        AUTO_DISC_MANAGER_MOCK.getRegTopicSocketInfos().forEach((autoDiscTopicSocketInfo ->
        {
            final AeronPublisherParams pubParams = new AeronPublisherParams(TransportMediaType.UNICAST, autoDiscTopicSocketInfo.getIpAddress(), autoDiscTopicSocketInfo.getPort(),
                    autoDiscTopicSocketInfo.getStreamId(), SUBNET_ADDRESS);
            final AeronPublisher publisher = new AeronPublisher(VEGA_CONTEXT, pubParams);
            publishersByTopic.put(autoDiscTopicSocketInfo.getTopicName(), publisher);
        }));

        // Give time to the connections to stabilise
        Thread.sleep(1000);

        // Now messages should arrive for all publishers
        publishersByTopic.forEach((topic, publisher) -> this.sendMessageAndCheckArrival(publisher, true));

        // Now unsubscribe from all
        subscriberManager.unsubscribeFromTopic("utopic1");
        subscriberManager.unsubscribeFromTopic("utopic2");
        subscriberManager.unsubscribeFromTopic("utopic3");
        subscriberManager.unsubscribeFromTopic("utopic4");
        publishersByTopic.forEach((topic, publisher) -> this.sendMessageAndCheckArrival(publisher, false));

        // Close all
        publishersByTopic.forEach((topic, publisher) -> publisher.close());
    }

    @Test
    public void testAdverts() {
        final UUID instanceId = UUID.randomUUID();
        final AutoDiscTopicSocketInfo topicSocketInfo1 = new AutoDiscTopicSocketInfo(instanceId, AutoDiscTransportType.PUB_MUL, UUID.randomUUID(), "mtopic1", UUID.randomUUID(), 34, 34534, 23, TestConstants.EMPTY_HOSTNAME);
        subscriberManager.onNewAutoDiscTopicSocketInfo(topicSocketInfo1);
        subscriberManager.onTimedOutAutoDiscTopicSocketInfo(topicSocketInfo1);
    }

    @Test
    public void testResponseSubParams() {
        Assert.assertEquals(subscriberManager.getResponsesSubscriberParams().getTransportType(), TransportMediaType.UNICAST);
    }

    private void sendMessageAndCheckArrival(final AeronPublisher aeronPublisher, boolean shouldArrive) {
        final UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocate(128));
        buffer.putInt(0, 128);

        aeronPublisher.sendMessage(MsgType.DATA, UUID.randomUUID(), buffer, new Random().nextLong(), 0, 4);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (shouldArrive) {
            Assert.assertNotNull(POLLER_LISTENER.receivedMsg);
        } else {
            assertNull(POLLER_LISTENER.receivedMsg);
        }

        POLLER_LISTENER.reset();
    }

    @Override
    public void onMessageReceived(IRcvMessage receivedMessage) {

    }

    @Override
    public void onRequestReceived(IRcvRequest receivedRequest) {

    }

    static class ReceiverListener implements ISubscribersPollerListener {
        volatile IRcvMessage receivedMsg = null;

        @Override
        public void onDataMsgReceived(RcvMessage msg) {
            this.receivedMsg = msg.promote();
        }

        @Override
        public void onEncryptedDataMsgReceived(RcvMessage msg) {

        }

        @Override
        public void onDataRequestMsgReceived(RcvRequest request) {
        }

        @Override
        public void onDataResponseMsgReceived(RcvResponse response) {
        }

        @Override
        public void onHeartbeatRequestMsgReceived(MsgReqHeader heartbeatReqMsgHeader) {

        }

        private void reset() {
            this.receivedMsg = null;
        }
    }
}