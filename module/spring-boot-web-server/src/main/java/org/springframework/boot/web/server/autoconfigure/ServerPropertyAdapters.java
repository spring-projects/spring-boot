/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.PropertyMapper.Source.Adapter;
import org.springframework.boot.web.server.ForwardedRequestHeaders;
import org.springframework.boot.web.server.autoconfigure.ServerProperties.Request.Header.Forwarded;
import org.springframework.core.env.Environment;

/**
 * {@link PropertyMapper} adapters for {@link ServerProperties}.
 *
 * @author Phillip Webb
 * @since 4.2.0
 */
public final class ServerPropertyAdapters {

	private ServerPropertyAdapters() {
	}

	/**
	 * Return an {@link Adapter} that converts {@link ServerProperties} to
	 * {@link ForwardedRequestHeaders}.
	 * @param environment the environment or {@code null}
	 * @return the properties adapter
	 */
	public static Adapter<ServerProperties, ForwardedRequestHeaders> webServerForwardedRequestHeaders(
			@Nullable Environment environment) {
		return new ForwardedRequestHeadersAdapter(environment);
	}

	/**
	 * {@link Adapter} that converts {@link ServerProperties} to
	 * {@link ForwardedRequestHeaders}.
	 */
	private record ForwardedRequestHeadersAdapter(
			@Nullable Environment environment) implements Adapter<ServerProperties, ForwardedRequestHeaders> {

		@Override
		@SuppressWarnings("removal")
		public @Nullable ForwardedRequestHeaders adapt(ServerProperties serverProperties) {
			ForwardedHeaderStrategy strategy = ForwardedHeaderStrategy.get(serverProperties);
			if (strategy == null) {
				CloudPlatform cloudPlatform = CloudPlatform.getActive(environment());
				strategy = (cloudPlatform != null && cloudPlatform.isUsingForwardHeaders())
						? ForwardedHeaderStrategy.WEB_SERVER : ForwardedHeaderStrategy.NONE;
			}
			return (strategy == ForwardedHeaderStrategy.WEB_SERVER) ? forwardedRequestHeaders(serverProperties) : null;
		}

		private ForwardedRequestHeaders forwardedRequestHeaders(ServerProperties serverProperties) {
			Forwarded forwardedProperties = serverProperties.getRequest().getHeader().getForwarded();
			return new ForwardedRequestHeaders(forwardedProperties.getNaming(), forwardedProperties.isUsePrefix());
		}

	}

}
