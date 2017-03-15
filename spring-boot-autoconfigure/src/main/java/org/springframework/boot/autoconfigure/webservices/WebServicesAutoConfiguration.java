/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.webservices;

import java.util.Map;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurationSupport;
import org.springframework.ws.transport.http.MessageDispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Web Services.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(MessageDispatcherServlet.class)
@ConditionalOnMissingBean(WsConfigurationSupport.class)
@EnableConfigurationProperties(WebServicesProperties.class)
@AutoConfigureAfter(ServletWebServerFactoryAutoConfiguration.class)
public class WebServicesAutoConfiguration {

	private final WebServicesProperties properties;

	public WebServicesAutoConfiguration(WebServicesProperties properties) {
		this.properties = properties;
	}

	@Bean
	public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
			ApplicationContext applicationContext) {
		MessageDispatcherServlet servlet = new MessageDispatcherServlet();
		servlet.setApplicationContext(applicationContext);
		String path = this.properties.getPath();
		String urlMapping = (path.endsWith("/") ? path + "*" : path + "/*");
		ServletRegistrationBean<MessageDispatcherServlet> registration = new ServletRegistrationBean<>(
				servlet, urlMapping);
		WebServicesProperties.Servlet servletProperties = this.properties.getServlet();
		registration.setLoadOnStartup(servletProperties.getLoadOnStartup());
		for (Map.Entry<String, String> entry : servletProperties.getInit().entrySet()) {
			registration.addInitParameter(entry.getKey(), entry.getValue());
		}
		return registration;
	}

	@Configuration
	@EnableWs
	protected static class WsConfiguration {

	}

}
