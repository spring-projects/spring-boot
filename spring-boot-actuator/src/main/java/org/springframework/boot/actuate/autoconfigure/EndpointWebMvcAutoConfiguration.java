/*
 * Copyright 2012-2015 the original author or authors.
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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;
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
 * @author Johannes Stelzer
 */
@Configuration
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
@ConditionalOnWebApplication
@AutoConfigureAfter({ PropertyPlaceholderAutoConfiguration.class,
	EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class,
	ManagementServerPropertiesAutoConfiguration.class,
	HypermediaAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class })
public class EndpointWebMvcAutoConfiguration implements ApplicationContextAware,
		SmartInitializingSingleton {

	private static Log logger = LogFactory.getLog(EndpointWebMvcAutoConfiguration.class);

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Conditional(OnManagementMvcCondition.class)
	@Configuration
	@Import(EndpointWebMvcImportSelector.class)
	protected static class EndpointWebMvcConfiguration {
	}

	@Bean
	public ManagementContextResolver managementContextResolver() {
		return new ManagementContextResolver(this.applicationContext);
	}

	@Override
	public void afterSingletonsInstantiated() {
		ManagementServerPort managementPort = ManagementServerPort
				.get(this.applicationContext);
		if (managementPort == ManagementServerPort.DIFFERENT
				&& this.applicationContext instanceof EmbeddedWebApplicationContext
				&& ((EmbeddedWebApplicationContext) this.applicationContext)
						.getEmbeddedServletContainer() != null) {
			createChildManagementContext();
		}
		if (managementPort == ManagementServerPort.SAME
				&& this.applicationContext.getEnvironment() instanceof ConfigurableEnvironment) {
			addLocalManagementPortPropertyAlias((ConfigurableEnvironment) this.applicationContext
					.getEnvironment());
		}
	}

	private void createChildManagementContext() {
		final AnnotationConfigEmbeddedWebApplicationContext childContext = new AnnotationConfigEmbeddedWebApplicationContext();
		childContext.setParent(this.applicationContext);
		childContext.setNamespace("management");
		childContext.setId(this.applicationContext.getId() + ":management");
		childContext.register(EndpointWebMvcChildContextConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				EmbeddedServletContainerAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);
		CloseEventPropagationListener
				.addIfPossible(this.applicationContext, childContext);
		try {
			childContext.refresh();
			managementContextResolver().setApplicationContext(childContext);
		}
		catch (RuntimeException ex) {
			// No support currently for deploying a war with management.port=<different>,
			// and this is the signature of that happening
			if (ex instanceof EmbeddedServletContainerException
					|| ex.getCause() instanceof EmbeddedServletContainerException) {
				logger.warn("Could not start embedded container (management endpoints are still available through JMX)");
			}
			else {
				throw ex;
			}
		}
	}

	/**
	 * Add an alias for 'local.management.port' that actually resolves using
	 * 'local.server.port'.
	 * @param environment the environment
	 */
	private void addLocalManagementPortPropertyAlias(
			final ConfigurableEnvironment environment) {
		environment.getPropertySources().addLast(
				new PropertySource<Object>("Management Server") {
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
	protected static class ApplicationContextFilterConfiguration {

		@Bean
		public Filter applicationContextIdFilter(ApplicationContext context) {
			return new ApplicationContextHeaderFilter(context);
		}

	}

	/**
	 * {@link OncePerRequestFilter} to add the {@literal X-Application-Context} if
	 * required.
	 */
	private static class ApplicationContextHeaderFilter extends OncePerRequestFilter {

		private final ApplicationContext applicationContext;

		private ManagementServerProperties properties;

		public ApplicationContextHeaderFilter(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Override
		protected void doFilterInternal(HttpServletRequest request,
				HttpServletResponse response, FilterChain filterChain)
				throws ServletException, IOException {
			if (this.properties == null) {
				this.properties = this.applicationContext
						.getBean(ManagementServerProperties.class);
			}
			if (this.properties.getAddApplicationContextHeader()) {
				response.addHeader("X-Application-Context",
						this.applicationContext.getId());
			}
			filterChain.doFilter(request, response);
		}

	}

	/**
	 * {@link ApplicationListener} to propagate the {@link ContextClosedEvent} from a
	 * parent to a child.
	 */
	private static class CloseEventPropagationListener implements
			ApplicationListener<ContextClosedEvent> {

		private final ApplicationContext parentContext;

		private final ConfigurableApplicationContext childContext;

		public CloseEventPropagationListener(ApplicationContext parentContext,
				ConfigurableApplicationContext childContext) {
			this.parentContext = parentContext;
			this.childContext = childContext;
		}

		@Override
		public void onApplicationEvent(ContextClosedEvent event) {
			if (event.getApplicationContext() == this.parentContext) {
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
			parentContext.addApplicationListener(new CloseEventPropagationListener(
					parentContext, childContext));
		}

	}

	protected static enum ManagementServerPort {

		DISABLE, SAME, DIFFERENT;

		public static ManagementServerPort get(BeanFactory beanFactory) {
			ServerProperties serverProperties;
			try {
				serverProperties = beanFactory.getBean(ServerProperties.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				serverProperties = new ServerProperties();
			}
			ManagementServerProperties managementServerProperties;
			try {
				managementServerProperties = beanFactory
						.getBean(ManagementServerProperties.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				managementServerProperties = new ManagementServerProperties();
			}
			Integer port = managementServerProperties.getPort();
			if (port != null && port < 0) {
				return DISABLE;
			}
			if (!(beanFactory instanceof WebApplicationContext)) {
				// Current context is not a webapp
				return DIFFERENT;
			}
			return ((port == null)
					|| (serverProperties.getPort() == null && port.equals(8080))
					|| (port != 0 && port.equals(serverProperties.getPort())) ? SAME
					: DIFFERENT);
		}

	}

	private static class OnManagementMvcCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			RelaxedPropertyResolver management = new RelaxedPropertyResolver(
					context.getEnvironment(), "management.");
			RelaxedPropertyResolver server = new RelaxedPropertyResolver(
					context.getEnvironment(), "server.");
			Integer managementPort = management.getProperty("port", Integer.class);
			if (managementPort == null) {
				ManagementServerProperties managementServerProperties = getBeanCarefully(
						context, ManagementServerProperties.class);
				if (managementServerProperties != null) {
					managementPort = managementServerProperties.getPort();
				}
			}
			if (managementPort != null && managementPort < 0) {
				return new ConditionOutcome(false, "The mangagement port is disabled");
			}
			if (!(context.getResourceLoader() instanceof WebApplicationContext)) {
				// Current context is not a webapp
				return new ConditionOutcome(false, "The context is not a webapp");
			}
			Integer serverPort = server.getProperty("port", Integer.class);
			if (serverPort == null) {
				ServerProperties serverProperties = getBeanCarefully(context,
						ServerProperties.class);
				if (serverProperties != null) {
					serverPort = serverProperties.getPort();
				}
			}
			if ((managementPort == null)
					|| (serverPort == null && managementPort.equals(8080))
					|| (managementPort != 0 && managementPort.equals(serverPort))) {
				return ConditionOutcome
						.match("The main context is the management context");
			}
			return ConditionOutcome
					.noMatch("The main context is not the management context");
		}

		private <T> T getBeanCarefully(ConditionContext context, Class<T> type) {
			String[] names = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
					context.getBeanFactory(), type, false, false);
			if (names.length == 1) {
				BeanDefinition original = findBeanDefinition(context.getBeanFactory(),
						names[0]);
				if (original instanceof RootBeanDefinition) {
					DefaultListableBeanFactory temp = new DefaultListableBeanFactory();
					temp.setParentBeanFactory(context.getBeanFactory());
					temp.registerBeanDefinition("bean",
							((RootBeanDefinition) original).cloneBeanDefinition());
					return temp.getBean(type);
				}
				return BeanFactoryUtils.beanOfType(context.getBeanFactory(), type, false,
						false);
			}
			;
			return null;
		}

		private BeanDefinition findBeanDefinition(
				ConfigurableListableBeanFactory beanFactory, String name) {
			BeanDefinition original = null;
			while (beanFactory != null && original == null) {
				if (beanFactory.containsLocalBean(name)) {
					original = beanFactory.getBeanDefinition(name);
				}
				else {
					BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();
					if (parentBeanFactory instanceof ConfigurableListableBeanFactory) {
						beanFactory = (ConfigurableListableBeanFactory) parentBeanFactory;
					}
					else {
						beanFactory = null;
					}
				}
			}
			return original;
		}

	}

}
