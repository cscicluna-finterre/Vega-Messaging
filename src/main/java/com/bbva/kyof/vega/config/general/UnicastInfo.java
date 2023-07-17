package com.bbva.kyof.vega.config.general;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contains the address and port for one resolver daemon
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UnicastInfo")
@NoArgsConstructor
@AllArgsConstructor
public class UnicastInfo {
    /**
     * (Optional, only unicast) The resolver daemon address
     */
    @XmlElement(name = "resolver_daemon_address")
    @Getter
    private String resolverDaemonAddress;

    /**
     * (Optional, only unicast) The resolver daemon port
     */
    @XmlElement(name = "resolver_daemon_port")
    @Getter
    @Setter
    private Integer resolverDaemonPort;
}
