/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.h2;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.h2.server.web.JakartaWebServlet;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties.Settings;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.log.LogMessage;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for H2's web console.
 *
 * @author Andy Wilkinson
 * @author Marten Deinum
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(JakartaWebServlet.class)
@ConditionalOnProperty(prefix = "spring.h2.console", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(H2ConsoleProperties.class)
public class H2ConsoleAutoConfiguration {

	private static final Log logger = LogFactory.getLog(H2ConsoleAutoConfiguration.class);

	@Bean
	public ServletRegistrationBean<JakartaWebServlet> h2Console(H2ConsoleProperties properties,
			ObjectProvider<DataSource> dataSource) {
		String path = properties.getPath();
		String urlMapping = path + (path.endsWith("/") ? "*" : "/*");
		ServletRegistrationBean<JakartaWebServlet> registration = new ServletRegistrationBean<>(new JakartaWebServlet(),
				urlMapping);
		configureH2ConsoleSettings(registration, properties.getSettings());
		if (logger.isInfoEnabled()) {
			withThreadContextClassLoader(getClass().getClassLoader(), () -> logDataSources(dataSource, path));
		}
		return registration;
	}

	private void withThreadContextClassLoader(ClassLoader classLoader, Runnable action) {
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			action.run();
		}
		finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

	private void logDataSources(ObjectProvider<DataSource> dataSource, String path) {
		List<String> urls = dataSource.orderedStream().map(this::getConnectionUrl).filter(Objects::nonNull).toList();
		if (!urls.isEmpty()) {
			logger.info(LogMessage.format("H2 console available at '%s'. %s available at %s", path,
					(urls.size() > 1) ? "Databases" : "Database", String.join(", ", urls)));
		}
	}

	private String getConnectionUrl(DataSource dataSource) {
		try (Connection connection = dataSource.getConnection()) {
			return "'" + connection.getMetaData().getURL() + "'";
		}
		catch (Exception ex) {
			return null;
		}
	}

	private void configureH2ConsoleSettings(ServletRegistrationBean<JakartaWebServlet> registration,
			Settings settings) {
		if (settings.isTrace()) {
			registration.addInitParameter("trace", "");
		}
		if (settings.isWebAllowOthers()) {
			registration.addInitParameter("webAllowOthers", "");
		}
		if (settings.getWebAdminPassword() != null) {
			registration.addInitParameter("webAdminPassword", settings.getWebAdminPassword());
		}
	}

}
