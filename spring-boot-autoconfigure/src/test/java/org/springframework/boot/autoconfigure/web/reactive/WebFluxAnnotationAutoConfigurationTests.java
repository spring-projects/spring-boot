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

package org.springframework.boot.autoconfigure.web.reactive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.validation.DelegatingValidator;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfigurationTests.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.boot.web.reactive.context.GenericReactiveWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.CompositeContentTypeResolver;
import org.springframework.web.reactive.config.WebFluxConfigurationSupport;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.resource.CachingResourceResolver;
import org.springframework.web.reactive.resource.CachingResourceTransformer;
import org.springframework.web.reactive.resource.PathResourceResolver;
import org.springframework.web.reactive.resource.ResourceWebHandler;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.view.ViewResolutionResultHandler;
import org.springframework.web.reactive.result.view.ViewResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebFluxAnnotationAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 */
public class WebFluxAnnotationAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private GenericReactiveWebApplicationContext context;

	@Test
	public void shouldNotProcessIfExistingWebReactiveConfiguration() throws Exception {
		load(WebFluxConfigurationSupport.class);
		assertThat(this.context.getBeansOfType(RequestMappingHandlerMapping.class).size())
				.isEqualTo(1);
		assertThat(this.context.getBeansOfType(RequestMappingHandlerAdapter.class).size())
				.isEqualTo(1);
	}

	@Test
	public void shouldCreateDefaultBeans() throws Exception {
		load();
		assertThat(this.context.getBeansOfType(RequestMappingHandlerMapping.class).size())
				.isEqualTo(1);
		assertThat(this.context.getBeansOfType(RequestMappingHandlerAdapter.class).size())
				.isEqualTo(1);
		assertThat(this.context.getBeansOfType(CompositeContentTypeResolver.class).size())
				.isEqualTo(1);
		assertThat(this.context.getBean("resourceHandlerMapping", HandlerMapping.class))
				.isNotNull();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldRegisterCustomHandlerMethodArgumentResolver() throws Exception {
		load(CustomArgumentResolvers.class);
		RequestMappingHandlerAdapter adapter = this.context
				.getBean(RequestMappingHandlerAdapter.class);
		assertThat((List<HandlerMethodArgumentResolver>) ReflectionTestUtils
				.getField(adapter.getArgumentResolverConfigurer(), "customResolvers"))
						.contains(
								this.context.getBean("firstResolver",
										HandlerMethodArgumentResolver.class),
						this.context.getBean("secondResolver",
								HandlerMethodArgumentResolver.class));
	}

	@Test
	public void shouldRegisterResourceHandlerMapping() throws Exception {
		load();
		SimpleUrlHandlerMapping hm = this.context.getBean("resourceHandlerMapping",
				SimpleUrlHandlerMapping.class);
		assertThat(hm.getUrlMap().get("/**")).isInstanceOf(ResourceWebHandler.class);
		ResourceWebHandler staticHandler = (ResourceWebHandler) hm.getUrlMap().get("/**");
		assertThat(staticHandler.getLocations()).hasSize(5);

		assertThat(hm.getUrlMap().get("/webjars/**"))
				.isInstanceOf(ResourceWebHandler.class);
		ResourceWebHandler webjarsHandler = (ResourceWebHandler) hm.getUrlMap()
				.get("/webjars/**");
		assertThat(webjarsHandler.getLocations()).hasSize(1);
		assertThat(webjarsHandler.getLocations().get(0))
				.isEqualTo(new ClassPathResource("/META-INF/resources/webjars/"));
	}

	@Test
	public void shouldMapResourcesToCustomPath() throws Exception {
		load("spring.webflux.static-path-pattern:/static/**");
		SimpleUrlHandlerMapping hm = this.context.getBean("resourceHandlerMapping",
				SimpleUrlHandlerMapping.class);
		assertThat(hm.getUrlMap().get("/static/**"))
				.isInstanceOf(ResourceWebHandler.class);
		ResourceWebHandler staticHandler = (ResourceWebHandler) hm.getUrlMap()
				.get("/static/**");
		assertThat(staticHandler.getLocations()).hasSize(5);
	}

	@Test
	public void shouldNotMapResourcesWhenDisabled() throws Exception {
		load("spring.resources.add-mappings:false");
		assertThat(this.context.getBean("resourceHandlerMapping"))
				.isNotInstanceOf(SimpleUrlHandlerMapping.class);
	}

	@Test
	public void resourceHandlerChainEnabled() throws Exception {
		load("spring.resources.chain.enabled:true");
		SimpleUrlHandlerMapping hm = this.context.getBean("resourceHandlerMapping",
				SimpleUrlHandlerMapping.class);
		assertThat(hm.getUrlMap().get("/**")).isInstanceOf(ResourceWebHandler.class);
		ResourceWebHandler staticHandler = (ResourceWebHandler) hm.getUrlMap().get("/**");
		assertThat(staticHandler.getResourceResolvers()).extractingResultOf("getClass")
				.containsOnly(CachingResourceResolver.class, PathResourceResolver.class);
		assertThat(staticHandler.getResourceTransformers()).extractingResultOf("getClass")
				.containsOnly(CachingResourceTransformer.class);
	}

	@Test
	public void shouldRegisterViewResolvers() throws Exception {
		load(ViewResolvers.class);
		ViewResolutionResultHandler resultHandler = this.context
				.getBean(ViewResolutionResultHandler.class);
		assertThat(resultHandler.getViewResolvers()).containsExactly(
				this.context.getBean("aViewResolver", ViewResolver.class),
				this.context.getBean("anotherViewResolver", ViewResolver.class));
	}

	@Test
	public void validatorWhenSuppliedByConfigurerShouldThrowException() throws Exception {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("unexpected validator configuration");
		load(ValidatorWebFluxConfigurer.class);
	}

	@Test
	public void validatorWhenAutoConfiguredShouldUseAlias() throws Exception {
		load();
		Object defaultValidator = this.context.getBean("defaultValidator");
		Object webFluxValidator = this.context.getBean("webFluxValidator");
		String[] jsrValidatorBeans = this.context
				.getBeanNamesForType(javax.validation.Validator.class);
		String[] springValidatorBeans = this.context.getBeanNamesForType(Validator.class);
		assertThat(webFluxValidator).isSameAs(defaultValidator);
		assertThat(springValidatorBeans).containsExactly("defaultValidator");
		assertThat(jsrValidatorBeans).containsExactly("defaultValidator");
	}

	@Test
	public void validatorWhenUserDefinedSpringOnlyShouldUseDefined() throws Exception {
		load(UserDefinedSpringOnlyValidator.class);
		Object customValidator = this.context.getBean("customValidator");
		Object webFluxValidator = this.context.getBean("webFluxValidator");
		String[] jsrValidatorBeans = this.context
				.getBeanNamesForType(javax.validation.Validator.class);
		String[] springValidatorBeans = this.context.getBeanNamesForType(Validator.class);
		assertThat(webFluxValidator).isSameAs(customValidator);
		assertThat(this.context.getBean(Validator.class)).isEqualTo(customValidator);
		assertThat(springValidatorBeans).containsExactly("customValidator");
		assertThat(jsrValidatorBeans).isEmpty();
	}

	@Test
	public void validatorWhenUserDefinedJsr303ShouldAdapt() throws Exception {
		load(UserDefinedJsr303Validator.class);
		Object customValidator = this.context.getBean("customValidator");
		Object webFluxValidator = this.context.getBean("webFluxValidator");
		String[] jsrValidatorBeans = this.context
				.getBeanNamesForType(javax.validation.Validator.class);
		String[] springValidatorBeans = this.context.getBeanNamesForType(Validator.class);
		assertThat(webFluxValidator).isNotSameAs(customValidator);
		assertThat(this.context.getBean(javax.validation.Validator.class))
				.isEqualTo(customValidator);
		assertThat(springValidatorBeans).containsExactly("jsr303ValidatorAdapter");
		assertThat(jsrValidatorBeans).containsExactly("customValidator");
	}

	@Test
	public void validatorWhenUserDefinedSingleJsr303AndSpringShouldUseDefined()
			throws Exception {
		load(UserDefinedSingleJsr303AndSpringValidator.class);
		Object customValidator = this.context.getBean("customValidator");
		Object webFluxValidator = this.context.getBean("webFluxValidator");
		String[] jsrValidatorBeans = this.context
				.getBeanNamesForType(javax.validation.Validator.class);
		String[] springValidatorBeans = this.context.getBeanNamesForType(Validator.class);
		assertThat(webFluxValidator).isSameAs(customValidator);
		assertThat(this.context.getBean(javax.validation.Validator.class))
				.isEqualTo(customValidator);
		assertThat(this.context.getBean(Validator.class)).isEqualTo(customValidator);
		assertThat(springValidatorBeans).containsExactly("customValidator");
		assertThat(jsrValidatorBeans).containsExactly("customValidator");
	}

	@Test
	public void validatorWhenUserDefinedJsr303AndSpringShouldUseDefined()
			throws Exception {
		load(UserDefinedJsr303AndSpringValidator.class);
		Object customJsrValidator = this.context.getBean("customJsrValidator");
		Object customSpringValidator = this.context.getBean("customSpringValidator");
		Object webFluxValidator = this.context.getBean("webFluxValidator");
		String[] jsrValidatorBeans = this.context
				.getBeanNamesForType(javax.validation.Validator.class);
		String[] springValidatorBeans = this.context.getBeanNamesForType(Validator.class);
		assertThat(customJsrValidator).isNotSameAs(customSpringValidator);
		assertThat(webFluxValidator).isSameAs(customSpringValidator);
		assertThat(this.context.getBean(javax.validation.Validator.class))
				.isEqualTo(customJsrValidator);
		assertThat(this.context.getBean(Validator.class))
				.isEqualTo(customSpringValidator);
		assertThat(springValidatorBeans).containsExactly("customSpringValidator");
		assertThat(jsrValidatorBeans).containsExactly("customJsrValidator");
	}

	@Test
	public void validatorWhenExcludingValidatorAutoConfigurationShouldUseWebFlux()
			throws Exception {
		load(null, new Class[] { ValidationAutoConfiguration.class });
		Object webFluxValidator = this.context.getBean("webFluxValidator");
		String[] jsrValidatorBeans = this.context
				.getBeanNamesForType(javax.validation.Validator.class);
		String[] springValidatorBeans = this.context.getBeanNamesForType(Validator.class);
		assertThat(webFluxValidator).isInstanceOf(DelegatingValidator.class);
		assertThat(springValidatorBeans).containsExactly("webFluxValidator");
		assertThat(jsrValidatorBeans).isEmpty();
	}

	@Test
	public void validatorWhenMultipleValidatorsAndNoWebFluxValidatorShouldAddWebFlux()
			throws Exception {
		load(MultipleValidatorsAndNoWebFluxValidator.class);
		Object customValidator1 = this.context.getBean("customValidator1");
		Object customValidator2 = this.context.getBean("customValidator2");
		Object webFluxValidator = this.context.getBean("webFluxValidator");
		String[] jsrValidatorBeans = this.context
				.getBeanNamesForType(javax.validation.Validator.class);
		String[] springValidatorBeans = this.context.getBeanNamesForType(Validator.class);
		assertThat(webFluxValidator).isNotSameAs(customValidator1)
				.isNotSameAs(customValidator2);
		assertThat(springValidatorBeans).containsExactly("customValidator1",
				"customValidator2", "webFluxValidator");
		assertThat(jsrValidatorBeans).isEmpty();
	}

	@Test
	public void validatorWhenMultipleValidatorsAndWebFluxValidatorShouldUseWebFlux()
			throws Exception {
		load(MultipleValidatorsAndWebFluxValidator.class);
		Object customValidator = this.context.getBean("customValidator");
		Object webFluxValidator = this.context.getBean("webFluxValidator");
		String[] jsrValidatorBeans = this.context
				.getBeanNamesForType(javax.validation.Validator.class);
		String[] springValidatorBeans = this.context.getBeanNamesForType(Validator.class);
		assertThat(webFluxValidator).isNotSameAs(customValidator);
		assertThat(springValidatorBeans).containsExactly("customValidator",
				"webFluxValidator");
		assertThat(jsrValidatorBeans).isEmpty();
	}

	private void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... environment) {
		load(config, null, environment);
	}

	private void load(Class<?> config, Class<?>[] exclude, String... environment) {
		this.context = new GenericReactiveWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, environment);
		List<Class<?>> configClasses = new ArrayList<>();
		if (config != null) {
			configClasses.add(config);
		}
		configClasses.addAll(Arrays.asList(Config.class,
				ValidationAutoConfiguration.class, BaseConfiguration.class));
		if (!ObjectUtils.isEmpty(exclude)) {
			configClasses.removeAll(Arrays.asList(exclude));
		}
		this.context.register(configClasses.toArray(new Class<?>[configClasses.size()]));
		this.context.refresh();

	}

	@Configuration
	protected static class CustomArgumentResolvers {

		@Bean
		public HandlerMethodArgumentResolver firstResolver() {
			return mock(HandlerMethodArgumentResolver.class);
		}

		@Bean
		public HandlerMethodArgumentResolver secondResolver() {
			return mock(HandlerMethodArgumentResolver.class);
		}

	}

	@Configuration
	protected static class ViewResolvers {

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		public ViewResolver aViewResolver() {
			return mock(ViewResolver.class);
		}

		@Bean
		public ViewResolver anotherViewResolver() {
			return mock(ViewResolver.class);
		}
	}

	@Configuration
	@Import({ WebFluxAnnotationAutoConfiguration.class })
	@EnableConfigurationProperties(WebFluxProperties.class)
	protected static class BaseConfiguration {

		@Bean
		public MockReactiveWebServerFactory mockReactiveWebServerFactory() {
			return new MockReactiveWebServerFactory();
		}

	}

	@Configuration
	protected static class CustomHttpHandler {

		@Bean
		public HttpHandler httpHandler() {
			return (serverHttpRequest, serverHttpResponse) -> null;
		}
	}

	@Configuration
	protected static class ValidatorWebFluxConfigurer implements WebFluxConfigurer {

		@Override
		public Optional<Validator> getValidator() {
			return Optional.of(mock(Validator.class));
		}

	}

	@Configuration
	static class UserDefinedSpringOnlyValidator {

		@Bean
		public Validator customValidator() {
			return mock(Validator.class);
		}

	}

	@Configuration
	static class UserDefinedJsr303Validator {

		@Bean
		public javax.validation.Validator customValidator() {
			return mock(javax.validation.Validator.class);
		}

	}

	@Configuration
	static class UserDefinedSingleJsr303AndSpringValidator {

		@Bean
		public LocalValidatorFactoryBean customValidator() {
			return new LocalValidatorFactoryBean();
		}

	}

	@Configuration
	static class UserDefinedJsr303AndSpringValidator {

		@Bean
		public javax.validation.Validator customJsrValidator() {
			return mock(javax.validation.Validator.class);
		}

		@Bean
		public Validator customSpringValidator() {
			return mock(Validator.class);
		}

	}

	@Configuration
	static class MultipleValidatorsAndNoWebFluxValidator {

		@Bean
		public Validator customValidator1() {
			return mock(Validator.class);
		}

		@Bean
		public Validator customValidator2() {
			return mock(Validator.class);
		}

	}

	@Configuration
	static class MultipleValidatorsAndWebFluxValidator {

		@Bean
		public Validator customValidator() {
			return mock(Validator.class);
		}

		@Bean
		public Validator webFluxValidator() {
			return mock(Validator.class);
		}

	}

}
