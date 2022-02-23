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

package org.springframework.boot.autoconfigure.webservices;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurationSupport;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.SimpleWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Web Services.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@AutoConfiguration(after = ServletWebServerFactoryAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(MessageDispatcherServlet.class)
@ConditionalOnMissingBean(WsConfigurationSupport.class)
@EnableConfigurationProperties(WebServicesProperties.class)
public class WebServicesAutoConfiguration {

	@Bean
	public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
			ApplicationContext applicationContext, WebServicesProperties properties) {
		MessageDispatcherServlet servlet = new MessageDispatcherServlet();
		servlet.setApplicationContext(applicationContext);
		String path = properties.getPath();
		String urlMapping = path + (path.endsWith("/") ? "*" : "/*");
		ServletRegistrationBean<MessageDispatcherServlet> registration = new ServletRegistrationBean<>(servlet,
				urlMapping);
		WebServicesProperties.Servlet servletProperties = properties.getServlet();
		registration.setLoadOnStartup(servletProperties.getLoadOnStartup());
		servletProperties.getInit().forEach(registration::addInitParameter);
		return registration;
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	@Conditional(OnWsdlLocationsCondition.class)
	public static WsdlDefinitionBeanFactoryPostProcessor wsdlDefinitionBeanFactoryPostProcessor() {
		return new WsdlDefinitionBeanFactoryPostProcessor();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWs
	protected static class WsConfiguration {

	}

	static class WsdlDefinitionBeanFactoryPostProcessor
			implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

		private ApplicationContext applicationContext;

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			Binder binder = Binder.get(this.applicationContext.getEnvironment());
			List<String> wsdlLocations = binder.bind("spring.webservices.wsdl-locations", Bindable.listOf(String.class))
					.orElse(Collections.emptyList());
			for (String wsdlLocation : wsdlLocations) {
				registerBeans(wsdlLocation, "*.wsdl", SimpleWsdl11Definition.class, SimpleWsdl11Definition::new,
						registry);
				registerBeans(wsdlLocation, "*.xsd", SimpleXsdSchema.class, SimpleXsdSchema::new, registry);
			}
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}

		private <T> void registerBeans(String location, String pattern, Class<T> type,
				Function<Resource, T> beanSupplier, BeanDefinitionRegistry registry) {
			for (Resource resource : getResources(location, pattern)) {
				BeanDefinition beanDefinition = BeanDefinitionBuilder
						.genericBeanDefinition(type, () -> beanSupplier.apply(resource)).getBeanDefinition();
				registry.registerBeanDefinition(StringUtils.stripFilenameExtension(resource.getFilename()),
						beanDefinition);
			}
		}

		private Resource[] getResources(String location, String pattern) {
			try {
				return this.applicationContext.getResources(ensureTrailingSlash(location) + pattern);
			}
			catch (IOException ex) {
				return new Resource[0];
			}
		}

		private String ensureTrailingSlash(String path) {
			return path.endsWith("/") ? path : path + "/";
		}

	}

}
