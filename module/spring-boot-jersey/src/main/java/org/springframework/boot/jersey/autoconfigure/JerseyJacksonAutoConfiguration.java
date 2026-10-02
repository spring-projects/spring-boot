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

package org.springframework.boot.jersey.autoconfigure;

import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.xml.bind.annotation.XmlElement;
import org.glassfish.jersey.internal.InternalProperties;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.jackson3.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule.Priority;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Jackson configuration shared by Jersey applications and management contexts.
 *
 * @author Kristoffer Larsen Hopland
 * @since 4.2.0
 */
@AutoConfiguration(before = JerseyAutoConfiguration.class,
		beforeName = "org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration",
		afterName = { "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration",
				"org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration",
				"org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration" })
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(ResourceConfig.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@Conditional(JerseyJacksonAutoConfiguration.JerseyResourcesAvailable.class)
public final class JerseyJacksonAutoConfiguration {

	private static void registerPreferredFeature(ResourceConfig config, Feature feature) {
		Feature delegate = config.getInstances()
			.stream()
			.filter(feature.getClass()::isInstance)
			.map(Feature.class::cast)
			.findFirst()
			.orElse(feature);
		String propertyName = PropertiesHelper.getPropertyNameForRuntime(InternalProperties.JSON_FEATURE,
				config.getRuntimeType());
		// Disable the discovered features before any of them can configure providers.
		config.property(propertyName, PreferredJacksonFeature.class.getName());
		config.register(new PreferredJacksonFeature(delegate), Ordered.HIGHEST_PRECEDENCE);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ JacksonFeature.class, JsonMapper.class })
	@ConditionalOnProperty(name = "spring.jersey.preferred-json-mapper", havingValue = "jackson", matchIfMissing = true)
	@ConditionalOnSingleCandidate(JsonMapper.class)
	static class JacksonResourceConfigCustomizerConfiguration {

		@Bean
		ResourceConfigCustomizer jacksonResourceConfigCustomizer(JsonMapper jsonMapper) {
			return (ResourceConfig config) -> {
				registerPreferredFeature(config, new JacksonFeature());
				config.register(new JsonMapperContextResolver(jsonMapper), ContextResolver.class);
			};
		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass({ JsonMapperBuilderCustomizer.class, JakartaXmlBindAnnotationModule.class,
				XmlElement.class })
		static class JaxbJsonMapperBuilderCustomizerConfiguration {

			@Bean
			JsonMapperBuilderCustomizer jaxbJsonMapperBuilderCustomizer(ObjectProvider<JacksonModule> modules) {
				return (builder) -> {
					if (modules.stream().noneMatch(JakartaXmlBindAnnotationModule.class::isInstance)) {
						builder.addModule(new JakartaXmlBindAnnotationModule().setPriority(Priority.SECONDARY));
					}
				};
			}

		}

		private static final class JsonMapperContextResolver implements ContextResolver<JsonMapper> {

			private final JsonMapper jsonMapper;

			private JsonMapperContextResolver(JsonMapper jsonMapper) {
				this.jsonMapper = jsonMapper;
			}

			@Override
			public JsonMapper getContext(Class<?> type) {
				return this.jsonMapper;
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ org.glassfish.jersey.jackson.JacksonFeature.class,
			com.fasterxml.jackson.databind.ObjectMapper.class })
	@Conditional(NoJacksonOrJackson2Preferred.class)
	@SuppressWarnings("removal")
	@ConditionalOnSingleCandidate(com.fasterxml.jackson.databind.ObjectMapper.class)
	static class Jackson2ResourceConfigCustomizerConfiguration {

		@Bean
		ResourceConfigCustomizer jackson2ResourceConfigCustomizer(
				com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
			return (ResourceConfig config) -> {
				registerPreferredFeature(config, new org.glassfish.jersey.jackson.JacksonFeature());
				config.register(new ObjectMapperContextResolver(objectMapper), ContextResolver.class);
			};
		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass({ com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector.class,
				XmlElement.class })
		static class JaxbJackson2ObjectMapperCustomizerConfiguration {

			@Autowired
			void addJaxbAnnotationIntrospector(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
				com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector jaxbAnnotationIntrospector = new com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector(
						objectMapper.getTypeFactory());
				objectMapper.setAnnotationIntrospectors(
						createPair(objectMapper.getSerializationConfig(), jaxbAnnotationIntrospector),
						createPair(objectMapper.getDeserializationConfig(), jaxbAnnotationIntrospector));
			}

			private com.fasterxml.jackson.databind.AnnotationIntrospector createPair(
					com.fasterxml.jackson.databind.cfg.MapperConfig<?> config,
					com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector jaxbAnnotationIntrospector) {
				return com.fasterxml.jackson.databind.AnnotationIntrospector.pair(config.getAnnotationIntrospector(),
						jaxbAnnotationIntrospector);
			}

		}

		private static final class ObjectMapperContextResolver
				implements ContextResolver<com.fasterxml.jackson.databind.ObjectMapper> {

			private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

			private ObjectMapperContextResolver(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
				this.objectMapper = objectMapper;
			}

			@Override
			public com.fasterxml.jackson.databind.ObjectMapper getContext(Class<?> type) {
				return this.objectMapper;
			}

		}

	}

	static class JerseyResourcesAvailable extends AnyNestedCondition {

		JerseyResourcesAvailable() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(ResourceConfig.class)
		static class ApplicationResources {

		}

		@ConditionalOnBean(type = "org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier")
		@ConditionalOnMissingClass("org.springframework.web.servlet.DispatcherServlet")
		static class ManagementResources {

		}

	}

	static class NoJacksonOrJackson2Preferred extends AnyNestedCondition {

		NoJacksonOrJackson2Preferred() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnMissingClass("tools.jackson.databind.json.JsonMapper")
		static class NoJackson {

		}

		@ConditionalOnProperty(name = "spring.jersey.preferred-json-mapper", havingValue = "jackson2")
		static class Jackson2Preferred {

		}

	}

	static final class PreferredJacksonFeature implements Feature {

		private final Feature delegate;

		PreferredJacksonFeature(Feature delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean configure(FeatureContext context) {
			String propertyName = PropertiesHelper.getPropertyNameForRuntime(InternalProperties.JSON_FEATURE,
					context.getConfiguration().getRuntimeType());
			context.property(propertyName, "JacksonFeature");
			boolean configured = this.delegate.configure(context);
			// Both Jersey Jackson features use the same JSON feature name. Configure
			// the preferred provider first, then prevent the other features from running,
			// including features that the application has already registered.
			context.property(propertyName, PreferredJacksonFeature.class.getName());
			return configured;
		}

	}

}
