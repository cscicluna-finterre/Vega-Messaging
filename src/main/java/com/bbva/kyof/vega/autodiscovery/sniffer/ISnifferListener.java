package com.bbva.kyof.vega.autodiscovery.sniffer;

import com.bbva.kyof.vega.autodiscovery.model.AutoDiscInstanceInfo;
import com.bbva.kyof.vega.autodiscovery.model.AutoDiscTopicInfo;
import com.bbva.kyof.vega.autodiscovery.model.AutoDiscTopicSocketInfo;

/**
 * Listener to receive the events triggered by the auto-discovery mechanism
 */
public interface ISnifferListener {
    /**
     * Called when a new autodiscovery topic info is added
     *
     * @param info the added info
     */
    void onNewAutoDiscTopicInfo(AutoDiscTopicInfo info);

    /**
     * Called when a autodiscovery topic info is removed due to expiration
     *
     * @param info the removed info
     */
    void onTimedOutAutoDiscTopicInfo(AutoDiscTopicInfo info);

    /**
     * Called when a new autodiscovery topic socket pair info is added
     *
     * @param info the added info
     */
    void onNewAutoDiscTopicSocketInfo(AutoDiscTopicSocketInfo info);

    /**
     * Called when a new autodiscovery topic socket pair info is removed due to expiration
     *
     * @param info the removed info
     */
    void onTimedOutAutoDiscTopicSocketInfo(AutoDiscTopicSocketInfo info);

    /**
     * Called when a new autodiscovery instance info is added
     *
     * @param info the added info
     */
    void onNewAutoDiscInstanceInfo(AutoDiscInstanceInfo info);

    /**
     * Called when a new autodiscovery instance info times out
     *
     * @param info the removed info
     */
    void onTimedOutAutoDiscInstanceInfo(AutoDiscInstanceInfo info);

}
