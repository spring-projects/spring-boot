package org.springframework.boot.actuate.autoconfigure;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Statsd metrics writer.
 *
 * @author Simon Buettner
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "statsd")
public class StatsdProperties {

    private static Log logger = LogFactory.getLog(StatsdProperties.class);

    private String host;

    private int port;

    private String prefix;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
