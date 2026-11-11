/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.autoconfigure.actuate.web.server;

import java.util.Collection;
import java.util.function.Function;

import org.apache.catalina.Valve;
import org.apache.catalina.valves.AccessLogValve;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.autoconfigure.web.server.AccessLogCustomizer;
import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory;

/**
 * {@link AccessLogCustomizer} for Tomcat.
 *
 * @param <T> the type of factory that can be customized
 * @author Andy Wilkinson
 */
class TomcatAccessLogCustomizer<T extends ConfigurableTomcatWebServerFactory> extends AccessLogCustomizer<T> {

	private final Function<T, Collection<Valve>> engineValvesExtractor;

	TomcatAccessLogCustomizer(TomcatManagementServerProperties properties,
			Function<T, Collection<Valve>> engineValvesExtractor) {
		super(properties.getAccesslog().getPrefix());
		this.engineValvesExtractor = engineValvesExtractor;
	}

	@Override
	public void customize(T factory) {
		AccessLogValve accessLogValve = findAccessLogValve(factory);
		if (accessLogValve == null) {
			return;
		}
		accessLogValve.setPrefix(customizePrefix(accessLogValve.getPrefix()));
	}

	private @Nullable AccessLogValve findAccessLogValve(T factory) {
		for (Valve engineValve : this.engineValvesExtractor.apply(factory)) {
			if (engineValve instanceof AccessLogValve accessLogValve) {
				return accessLogValve;
			}
		}
		return null;
	}

}
