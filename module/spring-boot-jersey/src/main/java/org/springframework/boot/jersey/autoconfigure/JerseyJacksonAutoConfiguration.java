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

import java.util.function.Supplier;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.xml.bind.annotation.XmlElement;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.jackson3.JacksonFeature;
import org.glassfish.jersey.jackson3.internal.jackson.jakarta.rs.base.StreamReadExceptionMapper;
import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.SingletonSupplier;

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

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ JacksonFeature.class, JsonMapper.class })
	static class JacksonResourceConfigCustomizerConfiguration {

		@Bean
		ResourceConfigCustomizer jacksonResourceConfigCustomizer(ObjectProvider<JsonMapper> jsonMappers) {
			return (ResourceConfig config) -> {
				config.register(JacksonFeature.class);
				config.register(JacksonExceptionMapperFeature.class, Ordered.LOWEST_PRECEDENCE);
				JsonMapper jsonMapper = jsonMappers.getIfUnique();
				if (jsonMapper != null) {
					config.register(new JsonMapperContextResolver(jsonMapper), ContextResolver.class);
				}
			};
		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass({ JsonMapperBuilderCustomizer.class, JakartaXmlBindAnnotationModule.class,
				XmlElement.class })
		static class JaxbJsonMapperBuilderCustomizerConfiguration {

			@Bean
			@Order(1)
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

			@Context
			private jakarta.ws.rs.core.@Nullable Configuration configuration;

			private final Supplier<JsonMapper> jerseyJsonMapper;

			private JsonMapperContextResolver(JsonMapper jsonMapper) {
				this.jsonMapper = jsonMapper;
				this.jerseyJsonMapper = SingletonSupplier.of(this::createJerseyJsonMapper);
			}

			@Override
			public JsonMapper getContext(Class<?> type) {
				return this.jerseyJsonMapper.get();
			}

			private JsonMapper createJerseyJsonMapper() {
				JsonMapper.Builder builder = this.jsonMapper.rebuild();
				boolean customized = false;
				if (ClassUtils.isPresent("tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule", null)
						&& ClassUtils.isPresent("jakarta.xml.bind.annotation.XmlElement", null)
						&& this.jsonMapper.serializationConfig()
							.getAnnotationIntrospector()
							.allIntrospectors()
							.stream()
							.noneMatch(JakartaXmlBindAnnotationIntrospector.class::isInstance)) {
					builder.addModule(new JakartaXmlBindAnnotationModule().setPriority(Priority.SECONDARY));
					customized = true;
				}
				if (this.configuration != null) {
					Integer maxStringLength = PropertiesHelper.convertValue(
							this.configuration.getProperty(MessageProperties.JSON_MAX_STRING_LENGTH), Integer.class);
					if (maxStringLength != null && maxStringLength != StreamReadConstraints.DEFAULT_MAX_STRING_LEN) {
						JsonFactory factory = this.jsonMapper.tokenStreamFactory();
						StreamReadConstraints constraints = factory.streamReadConstraints()
							.rebuild()
							.maxStringLength(maxStringLength)
							.build();
						builder = new JerseyJsonMapperBuilder(builder,
								factory.rebuild().streamReadConstraints(constraints).build());
						customized = true;
					}
				}
				return customized ? builder.build() : this.jsonMapper;
			}

		}

		private static final class JerseyJsonMapperBuilder extends JsonMapper.Builder {

			private JerseyJsonMapperBuilder(JsonMapper.Builder builder, JsonFactory factory) {
				super(new StateImpl(builder));
				// Jackson 3's builder has no public setter for its immutable stream
				// factory.
				this._streamFactory = factory;
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ org.glassfish.jersey.jackson.JacksonFeature.class,
			com.fasterxml.jackson.databind.ObjectMapper.class })
	@ConditionalOnMissingClass("org.glassfish.jersey.jackson3.JacksonFeature")
	@ConditionalOnSingleCandidate(com.fasterxml.jackson.databind.ObjectMapper.class)
	@SuppressWarnings("removal")
	static class Jackson2ResourceConfigCustomizerConfiguration {

		@Bean
		ResourceConfigCustomizer jackson2ResourceConfigCustomizer(
				com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
			return (ResourceConfig config) -> {
				config.register(org.glassfish.jersey.jackson.JacksonFeature.class);
				config.register(new ObjectMapperContextResolver(objectMapper), ContextResolver.class);
			};
		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass({ com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector.class,
				XmlElement.class })
		@ConditionalOnSingleCandidate(com.fasterxml.jackson.databind.ObjectMapper.class)
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

	private static final class JacksonExceptionMapperFeature implements Feature {

		@Override
		public boolean configure(FeatureContext context) {
			if (context.getConfiguration().isRegistered(StreamReadExceptionMapper.class)) {
				context.register(StreamConstraintsExceptionMapper.class);
			}
			return true;
		}

	}

	private static final class StreamConstraintsExceptionMapper implements ExceptionMapper<StreamConstraintsException> {

		@Override
		public Response toResponse(StreamConstraintsException exception) {
			return Response.status(Response.Status.BAD_REQUEST)
				.type(MediaType.TEXT_PLAIN)
				.entity(exception.getMessage())
				.build();
		}

	}

}
