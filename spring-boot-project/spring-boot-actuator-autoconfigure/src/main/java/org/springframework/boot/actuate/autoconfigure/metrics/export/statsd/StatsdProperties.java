package org.springframework.boot.actuate.autoconfigure.metrics.export.statsd;

import java.time.Duration;

import io.micrometer.statsd.StatsdFlavor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring Influx metrics export.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.metrics.statsd")
public class StatsdProperties {
	/**
	 * Enable publishing to the backend.
	 */
	private Boolean enabled = true;

	/**
	 * Choose which variant of the StatsD line protocol to use.
	 */
	private StatsdFlavor flavor = StatsdFlavor.Datadog;

	/**
	 * The host name of the StatsD agent.
	 */
	private String host = "localhost";

	/**
	 * The UDP port of the StatsD agent.
	 */
	private Integer port = 8125;

	/**
	 * The total length of a single payload should be kept within your network's MTU.
	 */
	private Integer maxPacketLength = 1400;

	/**
	 * Determines how often gauges will be polled. When a gauge is polled, its value is
	 * recalculated. If the value has changed, it is sent to the StatsD server.
	 */
	private Duration pollingFrequency = Duration.ofSeconds(10);

	/**
	 * Governs the maximum size of the queue of items waiting to be sent to a StatsD agent
	 * over UDP.
	 */
	private Integer queueSize = Integer.MAX_VALUE;

	/**
	 * Used to create a bucket filter clamping the bucket domain of timer percentiles
	 * histograms to some max value. This is used to limit the number of buckets shipped
	 * to Prometheus to save on storage.
	 */
	private Duration timerPercentilesMax = Duration.ofMinutes(2);

	/**
	 * Used to create a bucket filter clamping the bucket domain of timer percentiles
	 * histograms to some min value. This is used to limit the number of buckets shipped
	 * to Prometheus to save on storage.
	 */
	private Duration timerPercentilesMin = Duration.ofMillis(10);

	public Duration getTimerPercentilesMax() {
		return timerPercentilesMax;
	}

	public void setTimerPercentilesMax(Duration timerPercentilesMax) {
		this.timerPercentilesMax = timerPercentilesMax;
	}

	public Duration getTimerPercentilesMin() {
		return timerPercentilesMin;
	}

	public void setTimerPercentilesMin(Duration timerPercentilesMin) {
		this.timerPercentilesMin = timerPercentilesMin;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public StatsdFlavor getFlavor() {
		return flavor;
	}

	public void setFlavor(StatsdFlavor flavor) {
		this.flavor = flavor;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public Integer getMaxPacketLength() {
		return maxPacketLength;
	}

	public void setMaxPacketLength(Integer maxPacketLength) {
		this.maxPacketLength = maxPacketLength;
	}

	public Duration getPollingFrequency() {
		return pollingFrequency;
	}

	public void setPollingFrequency(Duration pollingFrequency) {
		this.pollingFrequency = pollingFrequency;
	}

	public Integer getQueueSize() {
		return queueSize;
	}

	public void setQueueSize(Integer queueSize) {
		this.queueSize = queueSize;
	}
}
