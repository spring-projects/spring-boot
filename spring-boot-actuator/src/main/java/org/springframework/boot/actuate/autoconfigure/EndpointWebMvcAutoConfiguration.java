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

package org.springframework.boot.actuate.autoconfigure;

import java.lang.reflect.Modifier;

import javax.servlet.Servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.ManagementServletContext;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.filter.ApplicationContextHeaderFilter;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to enable Spring MVC to handle
 * {@link Endpoint} requests. If the {@link ManagementServerProperties} specifies a
 * different port to {@link ServerProperties} a new child context is created, otherwise it
 * is assumed that endpoint requests will be mapped and handled via an already registered
 * {@link DispatcherServlet}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Johannes Edmeier
 * @author Eddú Meléndez
 * @author Venil Noronha
 * @author Madhura Bhave
 */
@Configuration
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(ManagementServerProperties.class)
@AutoConfigureAfter({ PropertyPlaceholderAutoConfiguration.class,
		ServletWebServerFactoryAutoConfiguration.class, WebMvcAutoConfiguration.class,
		RepositoryRestMvcAutoConfiguration.class, HypermediaAutoConfiguration.class,
		HttpMessageConvertersAutoConfiguration.class })
public class EndpointWebMvcAutoConfiguration
		implements ApplicationContextAware, SmartInitializingSingleton {

	private static final Log logger = LogFactory
			.getLog(EndpointWebMvcAutoConfiguration.class);

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Bean
	public ManagementContextResolver managementContextResolver() {
		return new ManagementContextResolver(this.applicationContext);
	}

	@Bean
	public ManagementServletContext managementServletContext(
			final ManagementServerProperties properties) {
		return new ManagementServletContext() {

			@Override
			public String getContextPath() {
				return properties.getContextPath();
			}

		};
	}

	@Override
	public void afterSingletonsInstantiated() {
		ManagementServerPort managementPort = ManagementServerPort.DIFFERENT;
		Environment environment = this.applicationContext.getEnvironment();
		if (this.applicationContext instanceof WebApplicationContext) {
			managementPort = ManagementServerPort.get(environment);
		}
		if (managementPort == ManagementServerPort.DIFFERENT) {
			if (this.applicationContext instanceof ServletWebServerApplicationContext
					&& ((ServletWebServerApplicationContext) this.applicationContext)
							.getWebServer() != null) {
				createChildManagementContext();
			}
			else {
				logger.warn("Could not start management web server on "
						+ "different port (management endpoints are still available "
						+ "through JMX)");
			}
		}
		if (managementPort == ManagementServerPort.SAME) {
			if (environment.getProperty("management.ssl.enabled") != null) {
				throw new IllegalStateException(
						"Management-specific SSL cannot be configured as the management "
								+ "server is not listening on a separate port");
			}
			if (environment instanceof ConfigurableEnvironment) {
				addLocalManagementPortPropertyAlias(
						(ConfigurableEnvironment) environment);
			}
		}
	}

	private void createChildManagementContext() {
		AnnotationConfigServletWebServerApplicationContext childContext = new AnnotationConfigServletWebServerApplicationContext();
		childContext.setParent(this.applicationContext);
		childContext.setNamespace("management");
		childContext.setId(this.applicationContext.getId() + ":management");
		childContext.setClassLoader(this.applicationContext.getClassLoader());
		childContext.register(EndpointWebMvcChildContextConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				ServletWebServerFactoryAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);
		registerServletWebServerFactory(childContext);
		CloseManagementContextListener.addIfPossible(this.applicationContext,
				childContext);
		childContext.refresh();
		managementContextResolver().setApplicationContext(childContext);
	}

	private void registerServletWebServerFactory(
			AnnotationConfigServletWebServerApplicationContext childContext) {
		try {
			ConfigurableListableBeanFactory beanFactory = childContext.getBeanFactory();
			if (beanFactory instanceof BeanDefinitionRegistry) {
				BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
				registry.registerBeanDefinition("ServletWebServerFactory",
						new RootBeanDefinition(determineServletWebServerFactoryClass()));
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Ignore and assume auto-configuration
		}
	}

	private Class<?> determineServletWebServerFactoryClass()
			throws NoSuchBeanDefinitionException {
		Class<?> factoryClass = this.applicationContext
				.getBean(ServletWebServerFactory.class).getClass();
		if (cannotBeInstantiated(factoryClass)) {
			throw new FatalBeanException("ServletWebServerFactory implementation "
					+ factoryClass.getName() + " cannot be instantiated. "
					+ "To allow a separate management port to be used, a top-level class "
					+ "or static inner class should be used instead");
		}
		return factoryClass;
	}

	private boolean cannotBeInstantiated(Class<?> clazz) {
		return clazz.isLocalClass()
				|| (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers()))
				|| clazz.isAnonymousClass();
	}

	/**
	 * Add an alias for 'local.management.port' that actually resolves using
	 * 'local.server.port'.
	 * @param environment the environment
	 */
	private void addLocalManagementPortPropertyAlias(
			final ConfigurableEnvironment environment) {
		environment.getPropertySources()
				.addLast(new PropertySource<Object>("Management Server") {
					@Override
					public Object getProperty(String name) {
						if ("local.management.port".equals(name)) {
							return environment.getProperty("local.server.port");
						}
						return null;
					}
				});
	}

	// Put Servlets and Filters in their own nested class so they don't force early
	// instantiation of ManagementServerProperties.
	@Configuration
	@ConditionalOnProperty(prefix = "management", name = "add-application-context-header", havingValue = "true")
	protected static class ApplicationContextFilterConfiguration {

		@Bean
		public ApplicationContextHeaderFilter applicationContextIdFilter(
				ApplicationContext context) {
			return new ApplicationContextHeaderFilter(context);
		}

	}

	@Configuration
	@Conditional(OnManagementMvcCondition.class)
	@Import(ManagementContextConfigurationsImportSelector.class)
	protected static class EndpointWebMvcConfiguration {

	}

	/**
	 * {@link ApplicationListener} to propagate the {@link ContextClosedEvent} and
	 * {@link ApplicationFailedEvent} from a parent to a child.
	 */
	private static class CloseManagementContextListener
			implements ApplicationListener<ApplicationEvent> {

		private final ApplicationContext parentContext;

		private final ConfigurableApplicationContext childContext;

		CloseManagementContextListener(ApplicationContext parentContext,
				ConfigurableApplicationContext childContext) {
			this.parentContext = parentContext;
			this.childContext = childContext;
		}

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof ContextClosedEvent) {
				onContextClosedEvent((ContextClosedEvent) event);
			}
			if (event instanceof ApplicationFailedEvent) {
				onApplicationFailedEvent((ApplicationFailedEvent) event);
			}
		};

		private void onContextClosedEvent(ContextClosedEvent event) {
			propagateCloseIfNecessary(event.getApplicationContext());
		}

		private void onApplicationFailedEvent(ApplicationFailedEvent event) {
			propagateCloseIfNecessary(event.getApplicationContext());
		}

		private void propagateCloseIfNecessary(ApplicationContext applicationContext) {
			if (applicationContext == this.parentContext) {
				this.childContext.close();
			}
		}

		public static void addIfPossible(ApplicationContext parentContext,
				ConfigurableApplicationContext childContext) {
			if (parentContext instanceof ConfigurableApplicationContext) {
				add((ConfigurableApplicationContext) parentContext, childContext);
			}
		}

		private static void add(ConfigurableApplicationContext parentContext,
				ConfigurableApplicationContext childContext) {
			parentContext.addApplicationListener(
					new CloseManagementContextListener(parentContext, childContext));
		}

	}

	private static class OnManagementMvcCondition extends SpringBootCondition
			implements ConfigurationCondition {

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage
					.forCondition("Management Server MVC");
			if (!(context.getResourceLoader() instanceof WebApplicationContext)) {
				return ConditionOutcome
						.noMatch(message.because("non WebApplicationContext"));
			}
			ManagementServerPort port = ManagementServerPort
					.get(context.getEnvironment());
			if (port == ManagementServerPort.SAME) {
				return ConditionOutcome.match(message.because("port is same"));
			}
			return ConditionOutcome.noMatch(message.because("port is not same"));
		}

	}

	protected enum ManagementServerPort {

		DISABLE, SAME, DIFFERENT;

		public static ManagementServerPort get(Environment environment) {
			Integer serverPort = getPortProperty(environment, "server.");
			Integer managementPort = getPortProperty(environment, "management.");
			if (managementPort != null && managementPort < 0) {
				return DISABLE;
			}
			return ((managementPort == null)
					|| (serverPort == null && managementPort.equals(8080))
					|| (managementPort != 0 && managementPort.equals(serverPort)) ? SAME
							: DIFFERENT);
		}

		private static Integer getPortProperty(Environment environment, String prefix) {
			return environment.getProperty(prefix + "port", Integer.class);
		}

	}

}
