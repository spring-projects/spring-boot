/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.jersey;

import java.util.Collections;
import java.util.EnumSet;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.xml.bind.annotation.XmlElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.SpringComponentProvider;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.ServletProperties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ConditionalOnMissingFilterBean;
import org.springframework.boot.autoconfigure.web.servlet.DefaultJerseyApplicationPath;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.DynamicRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ClassUtils;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.filter.RequestContextFilter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Jersey.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 1.2.0
 */
@AutoConfiguration(before = DispatcherServletAutoConfiguration.class, after = JacksonAutoConfiguration.class)
@ConditionalOnClass({ SpringComponentProvider.class, ServletRegistration.class })
@ConditionalOnBean(type = "org.glassfish.jersey.server.ResourceConfig")
@ConditionalOnWebApplication(type = Type.SERVLET)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(JerseyProperties.class)
public class JerseyAutoConfiguration implements ServletContextAware {

	private static final Log logger = LogFactory.getLog(JerseyAutoConfiguration.class);

	private final JerseyProperties jersey;

	private final ResourceConfig config;

	/**
	 * Constructs a new JerseyAutoConfiguration object with the specified
	 * JerseyProperties, ResourceConfig, and ResourceConfigCustomizer.
	 * @param jersey the JerseyProperties object containing the configuration properties
	 * for Jersey
	 * @param config the ResourceConfig object representing the configuration for Jersey
	 * @param customizers the ObjectProvider of ResourceConfigCustomizer objects used to
	 * customize the ResourceConfig
	 */
	public JerseyAutoConfiguration(JerseyProperties jersey, ResourceConfig config,
			ObjectProvider<ResourceConfigCustomizer> customizers) {
		this.jersey = jersey;
		this.config = config;
		customizers.orderedStream().forEach((customizer) -> customizer.customize(this.config));
	}

	/**
	 * Registers the RequestContextFilter as a filter bean if no other bean of type
	 * RequestContextFilter is present.
	 * @return The FilterRegistrationBean for the RequestContextFilter.
	 */
	@Bean
	@ConditionalOnMissingFilterBean(RequestContextFilter.class)
	public FilterRegistrationBean<RequestContextFilter> requestContextFilter() {
		FilterRegistrationBean<RequestContextFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(new RequestContextFilter());
		registration.setOrder(this.jersey.getFilter().getOrder() - 1);
		registration.setName("requestContextFilter");
		return registration;
	}

	/**
	 * Creates a new instance of {@link JerseyApplicationPath} if no bean of this type
	 * exists in the application context. The created instance uses the provided
	 * {@link Jersey} configuration and the application path specified in the
	 * configuration.
	 * @return a new instance of {@link JerseyApplicationPath} with the specified
	 * configuration
	 */
	@Bean
	@ConditionalOnMissingBean
	public JerseyApplicationPath jerseyApplicationPath() {
		return new DefaultJerseyApplicationPath(this.jersey.getApplicationPath(), this.config);
	}

	/**
	 * Creates a FilterRegistrationBean for the Jersey ServletContainer filter. This
	 * method is conditional on the absence of a bean named "jerseyFilterRegistration" and
	 * the presence of a property "spring.jersey.type" with the value "filter". The
	 * registration is configured with the provided JerseyApplicationPath and other
	 * properties from the configuration. The filter is set to the ServletContainer with
	 * the provided config. The URL patterns are set to the applicationPath's URL mapping.
	 * The order is set to the order specified in the jersey filter configuration. The
	 * filter's context path is set to the stripped pattern of the applicationPath's path.
	 * Additional init parameters are added to the registration. The name of the
	 * registration is set to "jerseyFilter". The dispatcher types are set to all
	 * dispatcher types.
	 * @param applicationPath the JerseyApplicationPath to configure the filter
	 * registration
	 * @return the created FilterRegistrationBean for the Jersey ServletContainer filter
	 */
	@Bean
	@ConditionalOnMissingBean(name = "jerseyFilterRegistration")
	@ConditionalOnProperty(prefix = "spring.jersey", name = "type", havingValue = "filter")
	public FilterRegistrationBean<ServletContainer> jerseyFilterRegistration(JerseyApplicationPath applicationPath) {
		FilterRegistrationBean<ServletContainer> registration = new FilterRegistrationBean<>();
		registration.setFilter(new ServletContainer(this.config));
		registration.setUrlPatterns(Collections.singletonList(applicationPath.getUrlMapping()));
		registration.setOrder(this.jersey.getFilter().getOrder());
		registration.addInitParameter(ServletProperties.FILTER_CONTEXT_PATH, stripPattern(applicationPath.getPath()));
		addInitParameters(registration);
		registration.setName("jerseyFilter");
		registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
		return registration;
	}

	/**
	 * Removes the trailing "/*" pattern from the given path.
	 * @param path the path to be stripped
	 * @return the stripped path
	 */
	private String stripPattern(String path) {
		if (path.endsWith("/*")) {
			path = path.substring(0, path.lastIndexOf("/*"));
		}
		return path;
	}

	/**
	 * Creates a ServletRegistrationBean for the Jersey servlet. This method is
	 * conditional on the absence of a bean named "jerseyServletRegistration" and the
	 * property "spring.jersey.type" having a value of "servlet" (or being missing).
	 * @param applicationPath the JerseyApplicationPath bean used to determine the URL
	 * mapping for the servlet
	 * @return the ServletRegistrationBean for the Jersey servlet
	 */
	@Bean
	@ConditionalOnMissingBean(name = "jerseyServletRegistration")
	@ConditionalOnProperty(prefix = "spring.jersey", name = "type", havingValue = "servlet", matchIfMissing = true)
	public ServletRegistrationBean<ServletContainer> jerseyServletRegistration(JerseyApplicationPath applicationPath) {
		ServletRegistrationBean<ServletContainer> registration = new ServletRegistrationBean<>(
				new ServletContainer(this.config), applicationPath.getUrlMapping());
		addInitParameters(registration);
		registration.setName(getServletRegistrationName());
		registration.setLoadOnStartup(this.jersey.getServlet().getLoadOnStartup());
		registration.setIgnoreRegistrationFailure(true);
		return registration;
	}

	/**
	 * Returns the name of the servlet registration for this configuration. The name is
	 * obtained by getting the user class of the configuration class and returning its
	 * name.
	 * @return the name of the servlet registration
	 */
	private String getServletRegistrationName() {
		return ClassUtils.getUserClass(this.config.getClass()).getName();
	}

	/**
	 * Adds the initialization parameters to the given DynamicRegistrationBean.
	 * @param registration the DynamicRegistrationBean to add the initialization
	 * parameters to
	 */
	private void addInitParameters(DynamicRegistrationBean<?> registration) {
		this.jersey.getInit().forEach(registration::addInitParameter);
	}

	/**
	 * Sets the ServletContext for the JerseyAutoConfiguration class.
	 * @param servletContext the ServletContext to be set
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		String servletRegistrationName = getServletRegistrationName();
		ServletRegistration registration = servletContext.getServletRegistration(servletRegistrationName);
		if (registration != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Configuring existing registration for Jersey servlet '" + servletRegistrationName + "'");
			}
			registration.setInitParameters(this.jersey.getInit());
		}
	}

	/**
	 * JerseyWebApplicationInitializer class.
	 */
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public static final class JerseyWebApplicationInitializer implements WebApplicationInitializer {

		/**
		 * This method is called during the startup of the application. It is used to
		 * switch off the Jersey WebApplicationInitializer by setting the
		 * "contextConfigLocation" parameter to "<NONE>". This is necessary because the
		 * Jersey WebApplicationInitializer tries to register a ContextLoaderListener
		 * which is not needed.
		 * @param servletContext the ServletContext object representing the application's
		 * servlet context
		 * @throws ServletException if an error occurs during the initialization process
		 */
		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {
			// We need to switch *off* the Jersey WebApplicationInitializer because it
			// will try and register a ContextLoaderListener which we don't need
			servletContext.setInitParameter("contextConfigLocation", "<NONE>");
		}

	}

	/**
	 * JacksonResourceConfigCustomizer class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JacksonFeature.class)
	@ConditionalOnSingleCandidate(ObjectMapper.class)
	static class JacksonResourceConfigCustomizer {

		/**
		 * Returns a customizer for configuring the ResourceConfig with Jackson.
		 * @param objectMapper the ObjectMapper instance to be used for JSON serialization
		 * and deserialization
		 * @return the customizer for configuring the ResourceConfig with Jackson
		 */
		@Bean
		ResourceConfigCustomizer jacksonResourceConfigCustomizer(ObjectMapper objectMapper) {
			return (ResourceConfig config) -> {
				config.register(JacksonFeature.class);
				config.register(new ObjectMapperContextResolver(objectMapper), ContextResolver.class);
			};
		}

		/**
		 * JaxbObjectMapperCustomizer class.
		 */
		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass({ JakartaXmlBindAnnotationIntrospector.class, XmlElement.class })
		static class JaxbObjectMapperCustomizer {

			/**
			 * Adds a JakartaXmlBindAnnotationIntrospector to the provided ObjectMapper.
			 * This introspector is used to handle JAXB annotations during serialization
			 * and deserialization.
			 * @param objectMapper the ObjectMapper to customize
			 */
			@Autowired
			void addJaxbAnnotationIntrospector(ObjectMapper objectMapper) {
				JakartaXmlBindAnnotationIntrospector jaxbAnnotationIntrospector = new JakartaXmlBindAnnotationIntrospector(
						objectMapper.getTypeFactory());
				objectMapper.setAnnotationIntrospectors(
						createPair(objectMapper.getSerializationConfig(), jaxbAnnotationIntrospector),
						createPair(objectMapper.getDeserializationConfig(), jaxbAnnotationIntrospector));
			}

			/**
			 * Creates a pair of AnnotationIntrospector by combining the given
			 * MapperConfig's AnnotationIntrospector with the
			 * JakartaXmlBindAnnotationIntrospector.
			 * @param config the MapperConfig from which to retrieve the
			 * AnnotationIntrospector
			 * @param jaxbAnnotationIntrospector the JakartaXmlBindAnnotationIntrospector
			 * to be combined with the AnnotationIntrospector from the MapperConfig
			 * @return the pair of AnnotationIntrospector created by combining the two
			 * provided introspectors
			 */
			private AnnotationIntrospector createPair(MapperConfig<?> config,
					JakartaXmlBindAnnotationIntrospector jaxbAnnotationIntrospector) {
				return AnnotationIntrospector.pair(config.getAnnotationIntrospector(), jaxbAnnotationIntrospector);
			}

		}

		/**
		 * ObjectMapperContextResolver class.
		 */
		private static final class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

			private final ObjectMapper objectMapper;

			/**
			 * Constructs a new ObjectMapperContextResolver with the specified
			 * ObjectMapper.
			 * @param objectMapper the ObjectMapper to be used by the context resolver
			 */
			private ObjectMapperContextResolver(ObjectMapper objectMapper) {
				this.objectMapper = objectMapper;
			}

			/**
			 * Returns the ObjectMapper instance associated with the specified class.
			 * @param type the class for which the ObjectMapper instance is requested
			 * @return the ObjectMapper instance associated with the specified class
			 */
			@Override
			public ObjectMapper getContext(Class<?> type) {
				return this.objectMapper;
			}

		}

	}

}
