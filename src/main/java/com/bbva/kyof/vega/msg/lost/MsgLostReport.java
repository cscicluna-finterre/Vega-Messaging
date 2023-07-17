package com.bbva.kyof.vega.msg.lost;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * Represents the class for any lost of data received message.
 * <p>
 * This class is not thread safe!
 */
@ToString
@AllArgsConstructor
public class MsgLostReport implements IMsgLostReport {
    /**
     * Unique Id of the library instance that sent the message
     */
    @Getter
    @Setter
    private UUID instanceId;

    /**
     * Topic name the message belongs to
     */
    @Getter
    @Setter
    private String topicName;

    /**
     * Number of messages that have been lost
     */
    @Getter
    @Setter
    private long numberLostMessages;

    /**
     * Id of the topic publisher they key belongs to
     */
    @Getter
    @Setter
    private UUID topicPublisherId;

}
