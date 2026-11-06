/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import org.apache.catalina.valves.RemoteIpValve;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.web.server.ForwardedRequestHeaders;
import org.springframework.util.Assert;

/**
 * Settings for Tomcat's {@link RemoteIpValve remote IPs}.
 *
 * @author Phillip Webb
 * @param header settings for the header or {@code null} to keep defaults
 * @param proxies setting for the proxies or {@code null} to keep defaults
 * @param usage when the configuration should be used
 * @since 4.2.0
 */
public record RemoteIp(@Nullable Header header, @Nullable Proxies proxies, Usage usage) {

	/**
	 * Use {@link RemoteIpValve} defaults.
	 */
	public static final RemoteIp DEFAULTS = new RemoteIp(null, null);

	public RemoteIp {
		Assert.notNull(usage, "'usage' must not be null");
	}

	public RemoteIp(@Nullable Header header, @Nullable Proxies proxies) {
		this(header, proxies, Usage.ALWAYS);
	}

	/**
	 * Remote IP header settings.
	 *
	 * @param host name of the HTTP header from which the remote host is extracted or
	 * {@code null} to keep default
	 * @param port name of the HTTP header used to override the original port value or
	 * {@code null} to keep default
	 * @param remoteIp name of the HTTP header from which the remote IP is extracted or
	 * {@code null} to keep default
	 * @param protocol name of the HTTP header that holds the incoming protocol or
	 * {@code null} to keep default
	 * @param protocolHttpsValue value of the protocol header indicating whether the
	 * incoming request uses SSL or {@code null} to keep default
	 */
	public record Header(@Nullable String host, @Nullable String port, @Nullable String remoteIp,
			@Nullable String protocol, @Nullable String protocolHttpsValue) {

		public Header(@Nullable String host, @Nullable String port, @Nullable String remoteIp,
				@Nullable String protocol) {
			this(host, port, remoteIp, protocol, null);
		}

	}

	/**
	 * Remote IP proxy settings.
	 *
	 * @param internal comma separate list of CIDR or a regular expression of the Internal
	 * proxies that are to be trusted or {@code null} to keep default
	 * @param trusted regular expression defining proxies that are trusted when they
	 * appear in the {@link Header#remoteIp()} header or {@code null} to keep default
	 */
	public record Proxies(@Nullable String internal, @Nullable String trusted) {

	}

	public enum Usage {

		/**
		 * Always configure the remote IP valve.
		 */
		ALWAYS,

		/**
		 * Only configure the remote IP value when the server has a non-null
		 * {@link ForwardedRequestHeaders} configuration.
		 */
		FOR_FORWARDED_REQUEST_HEADERS

	}

}
