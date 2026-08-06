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

package org.springframework.boot.sni.server;

import java.util.function.Consumer;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.tomcat.util.net.SSLHostConfig;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.tomcat.TomcatWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Applies a customization to each of Tomcat's {@code SSLHostConfig} instances and reports
 * their state once the server is running and again after an SSL bundle has been reloaded.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(TomcatWebServerFactory.class)
class TomcatSslHostConfigConfiguration {

	private static final int CUSTOMIZED_SESSION_TIMEOUT = 12345;

	@Bean
	WebServerFactoryCustomizer<TomcatWebServerFactory> sslHostConfigCustomizer() {
		return (factory) -> factory.addConnectorCustomizers(new SslHostConfigCustomizer());
	}

	@Bean
	ApplicationListener<WebServerInitializedEvent> sslHostConfigReporter(SslBundles sslBundles) {
		return new SslHostConfigReporter(sslBundles);
	}

	private static void forEachSslHostConfig(Connector connector, Consumer<SSLHostConfig> action) {
		if (connector.getProtocolHandler() instanceof AbstractHttp11Protocol<?> protocol) {
			for (SSLHostConfig sslHostConfig : protocol.findSslHostConfigs()) {
				action.accept(sslHostConfig);
			}
		}
	}

	static final class SslHostConfigCustomizer implements TomcatConnectorCustomizer {

		@Override
		public void customize(Connector connector) {
			forEachSslHostConfig(connector,
					(sslHostConfig) -> sslHostConfig.setSessionTimeout(CUSTOMIZED_SESSION_TIMEOUT));
		}

	}

	static final class SslHostConfigReporter implements ApplicationListener<WebServerInitializedEvent> {

		private final SslBundles sslBundles;

		SslHostConfigReporter(SslBundles sslBundles) {
			this.sslBundles = sslBundles;
		}

		@Override
		public void onApplicationEvent(WebServerInitializedEvent event) {
			WebServer webServer = event.getWebServer();
			if (!(webServer instanceof TomcatWebServer tomcatWebServer)) {
				return;
			}
			Connector connector = tomcatWebServer.getTomcat().getConnector();
			report(connector, "start");
			reloadBundles();
			report(connector, "reload");
		}

		private void reloadBundles() {
			if (!(this.sslBundles instanceof SslBundleRegistry registry)) {
				return;
			}
			for (String name : new String[] { "default", "alt" }) {
				registry.updateBundle(name, this.sslBundles.getBundle(name));
			}
		}

		private void report(Connector connector, String phase) {
			forEachSslHostConfig(connector,
					(sslHostConfig) -> System.out.println(">>>>> on " + phase + ", host="
							+ sslHostConfig.getHostName() + ", port=" + connector.getPort()
							+ ", sessionTimeout=" + sslHostConfig.getSessionTimeout()
							+ ", certificates.size=" + sslHostConfig.getCertificates().size()));
		}

	}

}
