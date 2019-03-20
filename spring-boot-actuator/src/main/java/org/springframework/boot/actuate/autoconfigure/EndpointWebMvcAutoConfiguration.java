/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.lang.reflect.Modifier;

import javax.servlet.Servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
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
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.web.filter.ApplicationContextHeaderFilter;
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
import org.springframework.core.type.MethodMetadata;
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
 */
@Configuration
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
@ConditionalOnWebApplication
@AutoConfigureAfter({ PropertyPlaceholderAutoConfiguration.class,
		EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class,
		ManagementServerPropertiesAutoConfiguration.class,
		RepositoryRestMvcAutoConfiguration.class, HypermediaAutoConfiguration.class,
		HttpMessageConvertersAutoConfiguration.class })
public class EndpointWebMvcAutoConfiguration
		implements ApplicationContextAware, BeanFactoryAware, SmartInitializingSingleton {

	private static final Log logger = LogFactory
			.getLog(EndpointWebMvcAutoConfiguration.class);

	private ApplicationContext applicationContext;

	private BeanFactory beanFactory;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
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
		if (this.applicationContext instanceof WebApplicationContext) {
			managementPort = ManagementServerPort
					.get(this.applicationContext.getEnvironment(), this.beanFactory);
		}
		if (managementPort == ManagementServerPort.DIFFERENT) {
			if (this.applicationContext instanceof EmbeddedWebApplicationContext
					&& ((EmbeddedWebApplicationContext) this.applicationContext)
							.getEmbeddedServletContainer() != null) {
				createChildManagementContext();
			}
			else {
				logger.warn("Could not start embedded management container on "
						+ "different port (management endpoints are still available "
						+ "through JMX)");
			}
		}
		if (managementPort == ManagementServerPort.SAME) {
			if (new RelaxedPropertyResolver(this.applicationContext.getEnvironment(),
					"management.ssl.").getProperty("enabled", Boolean.class, false)) {
				throw new IllegalStateException(
						"Management-specific SSL cannot be configured as the management "
								+ "server is not listening on a separate port");
			}
			if (this.applicationContext
					.getEnvironment() instanceof ConfigurableEnvironment) {
				addLocalManagementPortPropertyAlias(
						(ConfigurableEnvironment) this.applicationContext
								.getEnvironment());
			}
		}
	}

	private void createChildManagementContext() {
		AnnotationConfigEmbeddedWebApplicationContext childContext = new AnnotationConfigEmbeddedWebApplicationContext();
		childContext.setParent(this.applicationContext);
		childContext.setNamespace("management");
		childContext.setId(this.applicationContext.getId() + ":management");
		childContext.setClassLoader(this.applicationContext.getClassLoader());
		childContext.register(EndpointWebMvcChildContextConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				EmbeddedServletContainerAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);
		registerEmbeddedServletContainerFactory(childContext);
		CloseManagementContextListener.addIfPossible(this.applicationContext,
				childContext);
		childContext.refresh();
		managementContextResolver().setApplicationContext(childContext);
	}

	private void registerEmbeddedServletContainerFactory(
			AnnotationConfigEmbeddedWebApplicationContext childContext) {
		try {
			ConfigurableListableBeanFactory beanFactory = childContext.getBeanFactory();
			if (beanFactory instanceof BeanDefinitionRegistry) {
				BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
				registry.registerBeanDefinition("embeddedServletContainerFactory",
						new RootBeanDefinition(
								determineEmbeddedServletContainerFactoryClass()));
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Ignore and assume auto-configuration
		}
	}

	private Class<?> determineEmbeddedServletContainerFactoryClass()
			throws NoSuchBeanDefinitionException {
		Class<?> servletContainerFactoryClass = this.applicationContext
				.getBean(EmbeddedServletContainerFactory.class).getClass();
		if (cannotBeInstantiated(servletContainerFactoryClass)) {
			throw new FatalBeanException("EmbeddedServletContainerFactory implementation "
					+ servletContainerFactoryClass.getName() + " cannot be instantiated. "
					+ "To allow a separate management port to be used, a top-level class "
					+ "or static inner class should be used instead");
		}
		return servletContainerFactoryClass;
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
	@ConditionalOnProperty(prefix = "management", name = "add-application-context-header", matchIfMissing = true, havingValue = "true")
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
			ManagementServerPort port = ManagementServerPort.get(context.getEnvironment(),
					context.getBeanFactory());
			if (port == ManagementServerPort.SAME) {
				return ConditionOutcome.match(message.because("port is same"));
			}
			return ConditionOutcome.noMatch(message.because("port is not same"));
		}

	}

	protected enum ManagementServerPort {

		DISABLE, SAME, DIFFERENT;

		public static ManagementServerPort get(Environment environment,
				BeanFactory beanFactory) {
			Integer serverPort = getPortProperty(environment, "server.");
			if (serverPort == null && hasCustomBeanDefinition(beanFactory,
					ServerProperties.class, ServerPropertiesAutoConfiguration.class)) {
				serverPort = getTemporaryBean(beanFactory, ServerProperties.class)
						.getPort();
			}
			Integer managementPort = getPortProperty(environment, "management.");
			if (managementPort == null && hasCustomBeanDefinition(beanFactory,
					ManagementServerProperties.class,
					ManagementServerPropertiesAutoConfiguration.class)) {
				managementPort = getTemporaryBean(beanFactory,
						ManagementServerProperties.class).getPort();
			}
			if (managementPort != null && managementPort < 0) {
				return DISABLE;
			}
			return ((managementPort == null)
					|| (serverPort == null && managementPort.equals(8080))
					|| (managementPort != 0) && managementPort.equals(serverPort)) ? SAME
							: DIFFERENT;
		}

		private static <T> T getTemporaryBean(BeanFactory beanFactory, Class<T> type) {
			if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
				return null;
			}
			ConfigurableListableBeanFactory listable = (ConfigurableListableBeanFactory) beanFactory;
			String[] names = listable.getBeanNamesForType(type, true, false);
			if (names == null || names.length != 1) {
				return null;
			}
			// Use a temporary child bean factory to avoid instantiating the bean in the
			// parent (it won't be bound to the environment yet)
			return createTemporaryBean(type, listable,
					listable.getBeanDefinition(names[0]));
		}

		private static <T> T createTemporaryBean(Class<T> type,
				ConfigurableListableBeanFactory parent, BeanDefinition definition) {
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(
					parent);
			beanFactory.registerBeanDefinition(type.getName(), definition);
			return beanFactory.getBean(type);
		}

		private static Integer getPortProperty(Environment environment, String prefix) {
			RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(environment,
					prefix);
			return resolver.getProperty("port", Integer.class);
		}

		private static <T> boolean hasCustomBeanDefinition(BeanFactory beanFactory,
				Class<T> type, Class<?> configClass) {
			if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
				return false;
			}
			return hasCustomBeanDefinition((ConfigurableListableBeanFactory) beanFactory,
					type, configClass);
		}

		private static <T> boolean hasCustomBeanDefinition(
				ConfigurableListableBeanFactory beanFactory, Class<T> type,
				Class<?> configClass) {
			String[] names = beanFactory.getBeanNamesForType(type, true, false);
			if (names == null || names.length != 1) {
				return false;
			}
			BeanDefinition definition = beanFactory.getBeanDefinition(names[0]);
			if (definition instanceof AnnotatedBeanDefinition) {
				MethodMetadata factoryMethodMetadata = ((AnnotatedBeanDefinition) definition)
						.getFactoryMethodMetadata();
				if (factoryMethodMetadata != null) {
					String className = factoryMethodMetadata.getDeclaringClassName();
					return !configClass.getName().equals(className);
				}
			}
			return true;
		}

	}

}
