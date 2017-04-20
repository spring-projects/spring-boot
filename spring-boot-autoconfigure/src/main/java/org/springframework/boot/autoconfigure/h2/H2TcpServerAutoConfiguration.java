/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.h2;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.h2.server.web.WebServlet;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for H2's tcp server.
 *
 * @author lvasek
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass(WebServlet.class)
@ConditionalOnProperty(prefix = "spring.h2.tcp", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(H2TcpServerProperties.class)
public class H2TcpServerAutoConfiguration {

	private static final Logger logger = LoggerFactory
			.getLogger(H2TcpServerServletListener.class);

	private final H2TcpServerProperties properties;

	public H2TcpServerAutoConfiguration(H2TcpServerProperties properties) {
		this.properties = properties;
	}

	@Bean
	public ServletListenerRegistrationBean h2TcpServerServletListener() {
		ServletListenerRegistrationBean registrationBean = new ServletListenerRegistrationBean();
		registrationBean.setListener(new H2TcpServerServletListener(this.properties));
		return registrationBean;
	}

	public static class H2TcpServerServletListener implements ServletContextListener {

		private final H2TcpServerProperties properties;
		private Server server;

		public H2TcpServerServletListener(H2TcpServerProperties properties) {
			this.properties = properties;
		}

		@Override
		public void contextInitialized(ServletContextEvent servletContextEvent) {
			try {
				this.server = Server.createTcpServer(constructStartProperties()).start();
				logger.info("H2 Database tcp server url {}", this.server.getURL());
			}
			catch (Exception e) {
				throw new RuntimeException("Unable to start H2 Database tcp server");
			}
		}

		@Override
		public void contextDestroyed(ServletContextEvent servletContextEvent) {
			if (this.server != null && this.server.isRunning(false)) {
				this.server.stop();
			}
		}

		private String[] constructStartProperties() {
			List<String> args = new LinkedList<>();
			args.add("-tcp");

			if (this.properties.isAllowOthers()) {
				args.add("-tcpAllowOthers");
			}
			if (this.properties.isDaemonThread()) {
				args.add("-tcpDaemon");
			}
			if (this.properties.isUseSsl()) {
				args.add("-tcpSSL");
			}

			args.add("-tcpPort");
			if (StringUtils.isEmpty(this.properties.getPort())) {
				throw new RuntimeException("Tcp port for h2 TCP server must be provided");
			}
			args.add(this.properties.getPort());

			if (!StringUtils.isEmpty(this.properties.getShutdownPassword())) {
				args.add("-tcpPassword");
				args.add(this.properties.getShutdownPassword());
			}

			return args.toArray(new String[args.size()]);
		}
	}
}
