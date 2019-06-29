/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.ContextResolver;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.ServletProperties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
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
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.DynamicRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
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
@Configuration
@ConditionalOnClass(
		name = { "org.glassfish.jersey.server.spring.SpringComponentProvider", "javax.servlet.ServletRegistration" })
@ConditionalOnBean(type = "org.glassfish.jersey.server.ResourceConfig")
@ConditionalOnWebApplication(type = Type.SERVLET)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(DispatcherServletAutoConfiguration.class)
@AutoConfigureAfter(JacksonAutoConfiguration.class)
@EnableConfigurationProperties(JerseyProperties.class)
public class JerseyAutoConfiguration implements ServletContextAware {

	private static final Log logger = LogFactory.getLog(JerseyAutoConfiguration.class);

	private final JerseyProperties jersey;

	private final ResourceConfig config;

	private final List<ResourceConfigCustomizer> customizers;

	public JerseyAutoConfiguration(JerseyProperties jersey, ResourceConfig config,
			ObjectProvider<List<ResourceConfigCustomizer>> customizers) {
		this.jersey = jersey;
		this.config = config;
		this.customizers = customizers.getIfAvailable();
	}

	@PostConstruct
	public void path() {
		customize();
	}

	private void customize() {
		if (this.customizers != null) {
			AnnotationAwareOrderComparator.sort(this.customizers);
			for (ResourceConfigCustomizer customizer : this.customizers) {
				customizer.customize(this.config);
			}
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public JerseyApplicationPath jerseyApplicationPath() {
		return this::resolveApplicationPath;
	}

	private String resolveApplicationPath() {
		if (StringUtils.hasLength(this.jersey.getApplicationPath())) {
			return this.jersey.getApplicationPath();
		}
		return findApplicationPath(
				AnnotationUtils.findAnnotation(this.config.getApplication().getClass(), ApplicationPath.class));
	}

	@Bean
	@ConditionalOnMissingBean
	public FilterRegistrationBean<RequestContextFilter> requestContextFilter() {
		FilterRegistrationBean<RequestContextFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(new RequestContextFilter());
		registration.setOrder(this.jersey.getFilter().getOrder() - 1);
		registration.setName("requestContextFilter");
		return registration;
	}

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

	private String stripPattern(String path) {
		if (path.endsWith("/*")) {
			path = path.substring(0, path.lastIndexOf("/*"));
		}
		return path;
	}

	@Bean
	@ConditionalOnMissingBean(name = "jerseyServletRegistration")
	@ConditionalOnProperty(prefix = "spring.jersey", name = "type", havingValue = "servlet", matchIfMissing = true)
	public ServletRegistrationBean<ServletContainer> jerseyServletRegistration(JerseyApplicationPath applicationPath) {
		ServletRegistrationBean<ServletContainer> registration = new ServletRegistrationBean<>(
				new ServletContainer(this.config), applicationPath.getUrlMapping());
		addInitParameters(registration);
		registration.setName(getServletRegistrationName());
		registration.setLoadOnStartup(this.jersey.getServlet().getLoadOnStartup());
		return registration;
	}

	private String getServletRegistrationName() {
		return ClassUtils.getUserClass(this.config.getClass()).getName();
	}

	private void addInitParameters(DynamicRegistrationBean<?> registration) {
		this.jersey.getInit().forEach(registration::addInitParameter);
	}

	private static String findApplicationPath(ApplicationPath annotation) {
		// Jersey doesn't like to be the default servlet, so map to /* as a fallback
		if (annotation == null) {
			return "/*";
		}
		return annotation.value();
	}

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

	@Order(Ordered.HIGHEST_PRECEDENCE)
	public static final class JerseyWebApplicationInitializer implements WebApplicationInitializer {

		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {
			// We need to switch *off* the Jersey WebApplicationInitializer because it
			// will try and register a ContextLoaderListener which we don't need
			servletContext.setInitParameter("contextConfigLocation", "<NONE>");
		}

	}

	@ConditionalOnClass(JacksonFeature.class)
	@ConditionalOnSingleCandidate(ObjectMapper.class)
	@Configuration
	static class JacksonResourceConfigCustomizer {

		private static final String JAXB_ANNOTATION_INTROSPECTOR_CLASS_NAME = "com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector";

		@Bean
		public ResourceConfigCustomizer resourceConfigCustomizer(final ObjectMapper objectMapper) {
			addJaxbAnnotationIntrospectorIfPresent(objectMapper);
			return (ResourceConfig config) -> {
				config.register(JacksonFeature.class);
				config.register(new ObjectMapperContextResolver(objectMapper), ContextResolver.class);
			};
		}

		private void addJaxbAnnotationIntrospectorIfPresent(ObjectMapper objectMapper) {
			if (ClassUtils.isPresent(JAXB_ANNOTATION_INTROSPECTOR_CLASS_NAME, getClass().getClassLoader())) {
				new ObjectMapperCustomizer().addJaxbAnnotationIntrospector(objectMapper);
			}
		}

		private static final class ObjectMapperCustomizer {

			private void addJaxbAnnotationIntrospector(ObjectMapper objectMapper) {
				JaxbAnnotationIntrospector jaxbAnnotationIntrospector = new JaxbAnnotationIntrospector(
						objectMapper.getTypeFactory());
				objectMapper.setAnnotationIntrospectors(
						createPair(objectMapper.getSerializationConfig(), jaxbAnnotationIntrospector),
						createPair(objectMapper.getDeserializationConfig(), jaxbAnnotationIntrospector));
			}

			private AnnotationIntrospector createPair(MapperConfig<?> config,
					JaxbAnnotationIntrospector jaxbAnnotationIntrospector) {
				return AnnotationIntrospector.pair(config.getAnnotationIntrospector(), jaxbAnnotationIntrospector);
			}

		}

		private static final class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

			private final ObjectMapper objectMapper;

			private ObjectMapperContextResolver(ObjectMapper objectMapper) {
				this.objectMapper = objectMapper;
			}

			@Override
			public ObjectMapper getContext(Class<?> type) {
				return this.objectMapper;
			}

		}

	}

}
