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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

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
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties.Security;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.actuate.endpoint.ShutdownEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMappingCustomizer;
import org.springframework.boot.actuate.endpoint.mvc.EnvironmentMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HealthMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MetricsMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.actuate.endpoint.mvc.ShutdownMvcEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.annotation.AnnotationAttributes;
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
 */
@Configuration
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
@ConditionalOnWebApplication
@AutoConfigureAfter({ PropertyPlaceholderAutoConfiguration.class,
		EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class,
		ManagementServerPropertiesAutoConfiguration.class })
@EnableConfigurationProperties({ HealthMvcEndpointProperties.class,
		MvcEndpointCorsProperties.class })
public class EndpointWebMvcAutoConfiguration implements ApplicationContextAware,
		SmartInitializingSingleton {

	private static Log logger = LogFactory.getLog(EndpointWebMvcAutoConfiguration.class);

	private ApplicationContext applicationContext;

	@Autowired
	private HealthMvcEndpointProperties healthMvcEndpointProperties;

	@Autowired
	private ManagementServerProperties managementServerProperties;

	@Autowired
	private MvcEndpointCorsProperties corsMvcEndpointProperties;

	@Autowired(required = false)
	private List<EndpointHandlerMappingCustomizer> mappingCustomizers;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Bean
	@ConditionalOnMissingBean
	public EndpointHandlerMapping endpointHandlerMapping() {
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(mvcEndpoints()
				.getEndpoints(), this.corsMvcEndpointProperties.toCorsConfiguration());
		boolean disabled = ManagementServerPort.get(this.applicationContext) != ManagementServerPort.SAME;
		mapping.setDisabled(disabled);
		if (!disabled) {
			mapping.setPrefix(this.managementServerProperties.getContextPath());
		}
		if (this.mappingCustomizers != null) {
			for (EndpointHandlerMappingCustomizer customizer : this.mappingCustomizers) {
				customizer.customize(mapping);
			}
		}
		return mapping;
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

	@Bean
	@ConditionalOnMissingBean
	public MvcEndpoints mvcEndpoints() {
		return new MvcEndpoints();
	}

	@Bean
	@ConditionalOnBean(EnvironmentEndpoint.class)
	@ConditionalOnEnabledEndpoint("env")
	public EnvironmentMvcEndpoint environmentMvcEndpoint(EnvironmentEndpoint delegate) {
		return new EnvironmentMvcEndpoint(delegate);
	}

	@Bean
	@ConditionalOnBean(HealthEndpoint.class)
	@ConditionalOnEnabledEndpoint("health")
	public HealthMvcEndpoint healthMvcEndpoint(HealthEndpoint delegate) {
		Security security = this.managementServerProperties.getSecurity();
		boolean secure = (security == null || security.isEnabled());
		HealthMvcEndpoint healthMvcEndpoint = new HealthMvcEndpoint(delegate, secure);
		if (this.healthMvcEndpointProperties.getMapping() != null) {
			healthMvcEndpoint.addStatusMapping(this.healthMvcEndpointProperties
					.getMapping());
		}
		return healthMvcEndpoint;
	}

	@Bean
	@ConditionalOnBean(MetricsEndpoint.class)
	@ConditionalOnEnabledEndpoint("metrics")
	public MetricsMvcEndpoint metricsMvcEndpoint(MetricsEndpoint delegate) {
		return new MetricsMvcEndpoint(delegate);
	}

	@Bean
	@ConditionalOnBean(ShutdownEndpoint.class)
	@ConditionalOnEnabledEndpoint(value = "shutdown", enabledByDefault = false)
	public ShutdownMvcEndpoint shutdownMvcEndpoint(ShutdownEndpoint delegate) {
		return new ShutdownMvcEndpoint(delegate);
	}

	private void createChildManagementContext() {

		final AnnotationConfigEmbeddedWebApplicationContext childContext = new AnnotationConfigEmbeddedWebApplicationContext();
		childContext.setParent(this.applicationContext);
		childContext.setNamespace("management");
		childContext.setId(this.applicationContext.getId() + ":management");

		// Register the ManagementServerChildContextConfiguration first followed
		// by various specific AutoConfiguration classes. NOTE: The child context
		// is intentionally not completely auto-configured.
		childContext.register(EndpointWebMvcChildContextConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				EmbeddedServletContainerAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);

		// Ensure close on the parent also closes the child
		if (this.applicationContext instanceof ConfigurableApplicationContext) {
			((ConfigurableApplicationContext) this.applicationContext)
					.addApplicationListener(new ApplicationListener<ContextClosedEvent>() {
						@Override
						public void onApplicationEvent(ContextClosedEvent event) {
							if (event.getApplicationContext() == EndpointWebMvcAutoConfiguration.this.applicationContext) {
								childContext.close();
							}
						}
					});
		}
		try {
			childContext.refresh();
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

	/**
	 * {@link Conditional} that checks whether or not an endpoint is enabled. Matches if
	 * the value of the {@code endpoints.<name>.enabled} property is {@code true}. Does
	 * not match if the property's value or {@code enabledByDefault} is {@code false}.
	 * Otherwise, matches if the value of the {@code endpoints.enabled} property is
	 * {@code true} or if the property is not configured.
	 *
	 * @since 1.2.4
	 */
	@Conditional(OnEnabledEndpointCondition.class)
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface ConditionalOnEnabledEndpoint {

		/**
		 * The name of the endpoint.
		 * @return The name of the endpoint
		 */
		public String value();

		/**
		 * Returns whether or not the endpoint is enabled by default.
		 * @return {@code true} if the endpoint is enabled by default, otherwise
		 * {@code false}
		 */
		public boolean enabledByDefault() default true;

	}

	private static class OnEnabledEndpointCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			AnnotationAttributes annotationAttributes = AnnotationAttributes
					.fromMap(metadata
							.getAnnotationAttributes(ConditionalOnEnabledEndpoint.class
									.getName()));
			String endpointName = annotationAttributes.getString("value");
			boolean enabledByDefault = annotationAttributes
					.getBoolean("enabledByDefault");
			ConditionOutcome specificEndpointOutcome = determineSpecificEndpointOutcome(
					endpointName, enabledByDefault, context);
			if (specificEndpointOutcome != null) {
				return specificEndpointOutcome;
			}
			return determineAllEndpointsOutcome(context);

		}

		private ConditionOutcome determineSpecificEndpointOutcome(String endpointName,
				boolean enabledByDefault, ConditionContext context) {
			RelaxedPropertyResolver endpointPropertyResolver = new RelaxedPropertyResolver(
					context.getEnvironment(), "endpoints." + endpointName + ".");
			if (endpointPropertyResolver.containsProperty("enabled") || !enabledByDefault) {
				boolean match = endpointPropertyResolver.getProperty("enabled",
						Boolean.class, enabledByDefault);
				return new ConditionOutcome(match, "The " + endpointName + " is "
						+ (match ? "enabled" : "disabled"));
			}
			return null;
		}

		private ConditionOutcome determineAllEndpointsOutcome(ConditionContext context) {
			RelaxedPropertyResolver allEndpointsPropertyResolver = new RelaxedPropertyResolver(
					context.getEnvironment(), "endpoints.");
			boolean match = Boolean.valueOf(allEndpointsPropertyResolver.getProperty(
					"enabled", "true"));
			return new ConditionOutcome(match, "All endpoints are "
					+ (match ? "enabled" : "disabled") + " by default");
		}

	}

}
