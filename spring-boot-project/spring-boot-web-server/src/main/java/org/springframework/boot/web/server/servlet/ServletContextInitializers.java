/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.server.servlet;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.SessionCookieConfig;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;

/**
 * The {@link ServletContextInitializer ServletContextInitializers} to apply to a servlet
 * {@link WebServer}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public final class ServletContextInitializers implements Iterable<ServletContextInitializer> {

	private final List<ServletContextInitializer> initializers;

	private ServletContextInitializers(List<ServletContextInitializer> initializers) {
		this.initializers = initializers;
	}

	@Override
	public Iterator<ServletContextInitializer> iterator() {
		return this.initializers.iterator();
	}

	/**
	 * Creates a new instance from the given {@code settings} and {@code initializers}.
	 * @param settings the settings
	 * @param initializers the initializers
	 * @return the new instance
	 */
	public static ServletContextInitializers from(ServletWebServerSettings settings,
			ServletContextInitializer... initializers) {
		List<ServletContextInitializer> mergedInitializers = new ArrayList<>();
		mergedInitializers
			.add((servletContext) -> settings.getInitParameters().forEach(servletContext::setInitParameter));
		mergedInitializers.add(new SessionConfiguringInitializer(settings.getSession()));
		mergedInitializers.addAll(Arrays.asList(initializers));
		mergedInitializers.addAll(settings.getInitializers());
		return new ServletContextInitializers(mergedInitializers);
	}

	private static final class SessionConfiguringInitializer implements ServletContextInitializer {

		private final Session session;

		private SessionConfiguringInitializer(Session session) {
			this.session = session;
		}

		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {
			if (this.session.getTrackingModes() != null) {
				servletContext.setSessionTrackingModes(unwrap(this.session.getTrackingModes()));
			}
			configureSessionCookie(servletContext.getSessionCookieConfig());
		}

		private void configureSessionCookie(SessionCookieConfig config) {
			Cookie cookie = this.session.getCookie();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(cookie::getName).to(config::setName);
			map.from(cookie::getDomain).to(config::setDomain);
			map.from(cookie::getPath).to(config::setPath);
			map.from(cookie::getHttpOnly).to(config::setHttpOnly);
			map.from(cookie::getSecure).to(config::setSecure);
			map.from(cookie::getMaxAge).asInt(Duration::getSeconds).to(config::setMaxAge);
			map.from(cookie::getPartitioned)
				.as(Object::toString)
				.to((partitioned) -> config.setAttribute("Partitioned", partitioned));
		}

		private Set<jakarta.servlet.SessionTrackingMode> unwrap(Set<Session.SessionTrackingMode> modes) {
			if (modes == null) {
				return null;
			}
			Set<jakarta.servlet.SessionTrackingMode> result = new LinkedHashSet<>();
			for (Session.SessionTrackingMode mode : modes) {
				result.add(jakarta.servlet.SessionTrackingMode.valueOf(mode.name()));
			}
			return result;
		}

	}

}
