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

import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.annotation.XmlElement;
import org.glassfish.jersey.innate.inject.BlindBinder;
import org.glassfish.jersey.jackson3.JacksonFeature;
import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule.Priority;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTP tests for Jersey's Jackson provider selection and customization.
 *
 * @author Kristoffer Larsen Hopland
 */
class JerseyJacksonAutoConfigurationIntegrationTests {

	@AfterEach
	void resetDefaultStreamReadConstraints() {
		// Jersey 4.0.3 changes Jackson 3's global defaults when a feature has a limit.
		StreamReadConstraints.overrideDefaultStreamReadConstraints(null);
	}

	@Test
	void featureStringLimitIsEnforced() {
		ResourceConfig config = new ResourceConfig(Endpoint.class);
		config.register(new JacksonFeature().maxStringLength(1024));
		runner(config).run((context) -> {
			assertThat(post(context, "a".repeat(10000))).isEqualTo(HttpStatus.BAD_REQUEST.value());
			assertThat(post(context, "a".repeat(100))).isEqualTo(HttpStatus.OK.value());
		});
	}

	@Test
	void exceptionMappersCanBeDisabled() {
		ResourceConfig config = new ResourceConfig(Endpoint.class)
			.register(JacksonFeature.withoutExceptionMappers().maxStringLength(1024));
		runner(config).run((context) -> {
			assertThat(post(context, "a".repeat(10000))).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
			assertThat(post(context, "a".repeat(100))).isEqualTo(HttpStatus.OK.value());
		});
	}

	@Test
	void resourceConfigStringLimitTakesPrecedenceAndRetainsOtherConstraints() {
		JsonFactory factory = JsonFactory.builder()
			.streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(5).maxStringLength(20000).build())
			.build();
		JsonMapper mapper = JsonMapper.builder(factory).build();
		ResourceConfig config = new ResourceConfig(Endpoint.class).register(new JacksonFeature().maxStringLength(20000))
			.property(MessageProperties.JSON_MAX_STRING_LENGTH, 1024);
		runner(config).withBean(JsonMapper.class, () -> mapper).run((context) -> {
			assertThat(post(context, "a".repeat(10000))).isEqualTo(HttpStatus.BAD_REQUEST.value());
			assertThat(mapper.tokenStreamFactory()).isSameAs(factory);
			assertThat(factory.streamReadConstraints().getMaxStringLength()).isEqualTo(20000);
			assertThat(factory.streamReadConstraints().getMaxNestingDepth()).isEqualTo(5);
			assertThat(mapper.serializationConfig().getAnnotationIntrospector().allIntrospectors())
				.noneMatch(tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector.class::isInstance);
			int status = client(context).post()
				.uri("/tree")
				.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
				.body("{\"firstName\":{\"a\":{\"b\":{\"c\":{\"d\":{\"e\":{}}}}}}}")
				.exchange((request, response) -> response.getStatusCode().value());
			assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value());
		});
	}

	@ParameterizedTest
	@MethodSource("mapperCandidates")
	void jackson3ProviderIsSelectedRegardlessOfMapperCandidates(int candidates) {
		WebApplicationContextRunner runner = runner(new ResourceConfig(Endpoint.class), candidates != 0);
		for (int i = 0; i < candidates; i++) {
			runner = runner.withBean("jsonMapper" + i, JsonMapper.class, JsonMapper::new);
		}
		runner.run((context) -> {
			assertThat(client(context).get().uri("/engine").retrieve().body(String.class))
				.isEqualTo("{\"first_name\":\"value\"}");
			ServletContainer servlet = (ServletContainer) context
				.getBean("jerseyServletRegistration", ServletRegistrationBean.class)
				.getServlet();
			assertThat(servlet).isNotNull();
			assertThat(servlet.getApplicationHandler().getConfiguration().getClasses())
				.filteredOn((type) -> type.getSimpleName().startsWith("DefaultJackson"))
				.singleElement()
				.satisfies((type) -> assertThat(type.getName()).contains(".jackson3."));
		});
	}

	static Stream<Arguments> mapperCandidates() {
		return Stream.of(0, 1, 2).map(Arguments::of);
	}

	@Test
	void jackson3IsUsedWithAmbiguousMappers() {
		runner(new ResourceConfig(Endpoint.class)).withBean("one", JsonMapper.class, JsonMapper::new)
			.withBean("two", JsonMapper.class, JsonMapper::new)
			.run((context) -> assertThat(client(context).get().uri("/engine").retrieve().body(String.class))
				.isEqualTo("{\"first_name\":\"value\"}"));
	}

	@Test
	void classRegisteredFeatureRetainsSettingsAndInjection() {
		FeatureDependency dependency = new FeatureDependency();
		Resource.Builder resource = Resource.builder("/dynamic");
		resource.addMethod("GET").produces(MediaType.APPLICATION_JSON).handledBy((request) -> new Input("value"));
		ResourceConfig config = new ResourceConfig(Endpoint.class).register(new BlindBinder() {
			@Override
			protected void configure() {
				bind(dependency).to(FeatureDependency.class);
			}
		})
			.register(CustomJacksonFeature.class)
			.registerResources(resource.build())
			.property("custom-property", "value");
		runner(config).run((context) -> {
			assertThat(post(context, "a".repeat(10000))).isEqualTo(HttpStatus.BAD_REQUEST.value());
			assertThat(post(context, "a".repeat(100))).isEqualTo(HttpStatus.OK.value());
			assertThat(client(context).get().uri("/dynamic").retrieve().body(String.class))
				.isEqualTo("{\"firstName\":\"value\"}");
			ServletContainer servlet = (ServletContainer) context
				.getBean("jerseyServletRegistration", ServletRegistrationBean.class)
				.getServlet();
			assertThat(servlet).isNotNull();
			assertThat(servlet.getApplicationHandler().getConfiguration().getProperty("custom-property"))
				.isEqualTo("value");
			assertThat(dependency.initializations).isEqualTo(1);
			assertThat(dependency.configurations).isEqualTo(1);
		});
		assertThat(dependency.destructions).isEqualTo(1);
	}

	@Test
	void jaxbModuleInstalledByCustomizerRetainsPriority() {
		runner(new ResourceConfig(Endpoint.class))
			.withBean(JsonMapperBuilderCustomizer.class,
					() -> (builder) -> builder
						.addModule(new JakartaXmlBindAnnotationModule().setPriority(Priority.PRIMARY)))
			.run((context) -> assertThat(client(context).get().uri("/jaxb-conflict").retrieve().body(String.class))
				.isEqualTo("{\"xml_name\":\"value\"}"));
	}

	@Test
	void customJsonMapperGetsJaxbSupportWithoutChangingSharedMapper() {
		JsonMapper mapper = JsonMapper.builder()
			.propertyNamingStrategy(tools.jackson.databind.PropertyNamingStrategies.SNAKE_CASE)
			.build();
		runner(new ResourceConfig(Endpoint.class)).withBean(JsonMapper.class, () -> mapper).run((context) -> {
			assertThat(client(context).get().uri("/jaxb").retrieve().body(String.class))
				.isEqualTo("{\"xml_name\":\"value\"}");
			assertThat(mapper.writeValueAsString(new XmlDto())).isEqualTo("{\"value\":\"value\"}");
			assertThat(client(context).get().uri("/input").retrieve().body(String.class))
				.isEqualTo("{\"first_name\":\"value\"}");
		});
	}

	@Test
	void customJsonMapperRetainsExplicitJaxbPriority() {
		JsonMapper mapper = JsonMapper.builder()
			.addModule(new JakartaXmlBindAnnotationModule().setPriority(Priority.PRIMARY))
			.build();
		runner(new ResourceConfig(Endpoint.class).register(new JacksonFeature().maxStringLength(1024)))
			.withBean(JsonMapper.class, () -> mapper)
			.run((context) -> assertThat(client(context).get().uri("/jaxb-conflict").retrieve().body(String.class))
				.isEqualTo("{\"xml_name\":\"value\"}"));
	}

	private WebApplicationContextRunner runner(ResourceConfig config) {
		return runner(config, true);
	}

	private WebApplicationContextRunner runner(ResourceConfig config, boolean autoConfigureMappers) {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
			.withConfiguration(AutoConfigurations.of(TomcatServletWebServerAutoConfiguration.class,
					JerseyAutoConfiguration.class, JerseyJacksonAutoConfiguration.class))
			.withBean(ResourceConfig.class, () -> config)
			.withPropertyValues("server.port=0");
		return autoConfigureMappers ? runner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
				: runner;
	}

	private RestClient client(AssertableWebApplicationContext context) {
		AnnotationConfigServletWebServerApplicationContext applicationContext = context
			.getSourceApplicationContext(AnnotationConfigServletWebServerApplicationContext.class);
		assertThat(applicationContext.getWebServer()).isNotNull();
		return RestClient.builder().baseUrl("http://localhost:" + applicationContext.getWebServer().getPort()).build();
	}

	private int post(AssertableWebApplicationContext context, String value) {
		return client(context).post()
			.uri("/echo")
			.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
			.body("{\"firstName\":\"" + value + "\"}")
			.exchange((request, response) -> response.getStatusCode().value());
	}

	@Path("/")
	public static class Endpoint {

		@GET
		@Path("/engine")
		@Produces(MediaType.APPLICATION_JSON)
		public Engine engine() {
			return new Engine("value");
		}

		@GET
		@Path("/input")
		@Produces(MediaType.APPLICATION_JSON)
		public Input input() {
			return new Input("value");
		}

		@GET
		@Path("/jaxb")
		@Produces(MediaType.APPLICATION_JSON)
		public XmlDto jaxb() {
			return new XmlDto();
		}

		@GET
		@Path("/jaxb-conflict")
		@Produces(MediaType.APPLICATION_JSON)
		public XmlConflictDto conflict() {
			return new XmlConflictDto();
		}

		@POST
		@Path("/tree")
		@Consumes(MediaType.APPLICATION_JSON)
		@Produces(MediaType.APPLICATION_JSON)
		public Object tree(Object input) {
			return input;
		}

		@POST
		@Path("/echo")
		@Consumes(MediaType.APPLICATION_JSON)
		@Produces(MediaType.APPLICATION_JSON)
		public Input echo(Input input) {
			return input;
		}

	}

	@tools.jackson.databind.annotation.JsonNaming(tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
	@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public record Engine(String firstName) {
	}

	public record Input(String firstName) {
	}

	public static class XmlDto {

		@XmlElement(name = "xml_name")
		public String getValue() {
			return "value";
		}

	}

	public static class XmlConflictDto {

		@XmlElement(name = "xml_name")
		@JsonProperty("json_name")
		public String getValue() {
			return "value";
		}

	}

	public static class FeatureDependency {

		private int initializations;

		private int configurations;

		private int destructions;

	}

	public static class CustomJacksonFeature extends JacksonFeature {

		private final FeatureDependency dependency;

		private boolean initialized;

		@Inject
		CustomJacksonFeature(FeatureDependency dependency) {
			this.dependency = dependency;
			maxStringLength(1024);
		}

		@PostConstruct
		void initialize() {
			this.initialized = true;
			this.dependency.initializations++;
		}

		@Override
		public boolean configure(FeatureContext context) {
			assertThat(this.dependency).isNotNull();
			assertThat(this.initialized).isTrue();
			this.dependency.configurations++;
			return super.configure(context);
		}

		@PreDestroy
		void destroy() {
			this.dependency.destructions++;
			assertThat(this.initialized).isTrue();
		}

	}

}
