/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.web.server.tomcat;

import java.util.Collection;
import java.util.function.Function;

import org.apache.catalina.Valve;
import org.apache.catalina.valves.AccessLogValve;

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

	private AccessLogValve findAccessLogValve(T factory) {
		for (Valve engineValve : this.engineValvesExtractor.apply(factory)) {
			if (engineValve instanceof AccessLogValve accessLogValve) {
				return accessLogValve;
			}
		}
		return null;
	}

}
