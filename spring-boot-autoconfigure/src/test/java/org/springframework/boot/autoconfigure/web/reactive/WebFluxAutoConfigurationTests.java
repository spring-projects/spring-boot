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

import javax.validation.ValidatorFactory;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidatorAdapter;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfigurationTests.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.boot.web.reactive.context.GenericReactiveWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link WebFluxAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 */
public class WebFluxAutoConfigurationTests {

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
		assertThat(this.context.getBeansOfType(RequestedContentTypeResolver.class).size())
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
		List<HandlerMethodArgumentResolver> customResolvers = (List<HandlerMethodArgumentResolver>) ReflectionTestUtils
				.getField(adapter.getArgumentResolverConfigurer(), "customResolvers");
		assertThat(customResolvers).contains(
				this.context.getBean("firstResolver",
						HandlerMethodArgumentResolver.class),
				this.context.getBean("secondResolver",
						HandlerMethodArgumentResolver.class));
	}

	@Test
	public void shouldCustomizeCodecs() throws Exception {
		load(CustomCodecCustomizers.class);
		CodecCustomizer codecCustomizer = this.context.getBean("firstCodecCustomizer",
				CodecCustomizer.class);
		assertThat(codecCustomizer).isNotNull();
		verify(codecCustomizer).customize(any(ServerCodecConfigurer.class));
	}

	@Test
	public void shouldRegisterResourceHandlerMapping() throws Exception {
		load();
		SimpleUrlHandlerMapping hm = this.context.getBean("resourceHandlerMapping",
				SimpleUrlHandlerMapping.class);
		assertThat(hm.getUrlMap().get("/**")).isInstanceOf(ResourceWebHandler.class);
		ResourceWebHandler staticHandler = (ResourceWebHandler) hm.getUrlMap().get("/**");
		assertThat(staticHandler.getLocations()).hasSize(4);
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
		assertThat(staticHandler.getLocations()).hasSize(4);
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
	public void validatorWhenNoValidatorShouldUseDefault() {
		load(null, new Class<?>[] { ValidationAutoConfiguration.class });
		assertThat(this.context.getBeansOfType(ValidatorFactory.class)).isEmpty();
		assertThat(this.context.getBeansOfType(javax.validation.Validator.class))
				.isEmpty();
		String[] springValidatorBeans = this.context.getBeanNamesForType(Validator.class);
		assertThat(springValidatorBeans).containsExactly("webFluxValidator");
	}

	@Test
	public void validatorWhenNoCustomizationShouldUseAutoConfigured() {
		load();
		String[] jsrValidatorBeans = this.context
				.getBeanNamesForType(javax.validation.Validator.class);
		String[] springValidatorBeans = this.context.getBeanNamesForType(Validator.class);
		assertThat(jsrValidatorBeans).containsExactly("defaultValidator");
		assertThat(springValidatorBeans).containsExactly("defaultValidator",
				"webFluxValidator");
		Validator validator = this.context.getBean("webFluxValidator", Validator.class);
		assertThat(validator).isInstanceOf(ValidatorAdapter.class);
		Object defaultValidator = this.context.getBean("defaultValidator");
		assertThat(((ValidatorAdapter) validator).getTarget()).isSameAs(defaultValidator);
		// Primary Spring validator is the one used by WebFlux behind the scenes
		assertThat(this.context.getBean(Validator.class)).isEqualTo(defaultValidator);
	}

	@Test
	public void validatorWithConfigurerShouldUseSpringValidator() {
		load(ValidatorWebFluxConfigurer.class,
				new Class<?>[] { ValidationAutoConfiguration.class });
		assertThat(this.context.getBeansOfType(ValidatorFactory.class)).isEmpty();
		assertThat(this.context.getBeansOfType(javax.validation.Validator.class))
				.isEmpty();
		String[] springValidatorBeans = this.context.getBeanNamesForType(Validator.class);
		assertThat(springValidatorBeans).containsExactly("webFluxValidator");
		assertThat(this.context.getBean("webFluxValidator")).isSameAs(
				this.context.getBean(ValidatorWebFluxConfigurer.class).validator);
	}

	@Test
	public void validatorWithConfigurerDoesNotExposeJsr303() {
		load(ValidatorJsr303WebFluxConfigurer.class,
				new Class<?>[] { ValidationAutoConfiguration.class });
		assertThat(this.context.getBeansOfType(ValidatorFactory.class)).isEmpty();
		assertThat(this.context.getBeansOfType(javax.validation.Validator.class))
				.isEmpty();
		String[] springValidatorBeans = this.context.getBeanNamesForType(Validator.class);
		assertThat(springValidatorBeans).containsExactly("webFluxValidator");
		Validator validator = this.context.getBean("webFluxValidator", Validator.class);
		assertThat(validator).isInstanceOf(ValidatorAdapter.class);
		assertThat(((ValidatorAdapter) validator).getTarget()).isSameAs(
				this.context.getBean(ValidatorJsr303WebFluxConfigurer.class).validator);
	}

	@Test
	public void validationCustomConfigurerTakesPrecedence() {
		load(ValidatorWebFluxConfigurer.class);
		assertThat(this.context.getBeansOfType(ValidatorFactory.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(javax.validation.Validator.class))
				.hasSize(1);
		String[] springValidatorBeans = this.context.getBeanNamesForType(Validator.class);
		assertThat(springValidatorBeans).containsExactly("defaultValidator",
				"webFluxValidator");
		assertThat(this.context.getBean("webFluxValidator")).isSameAs(
				this.context.getBean(ValidatorWebFluxConfigurer.class).validator);
		// Primary Spring validator is the auto-configured one as the WebFlux one has been
		// customized via a WebFluxConfigurer
		assertThat(this.context.getBean(Validator.class))
				.isEqualTo(this.context.getBean("defaultValidator"));
	}

	@Test
	public void validatorWithCustomSpringValidatorIgnored() {
		load(CustomSpringValidator.class);
		String[] jsrValidatorBeans = this.context
				.getBeanNamesForType(javax.validation.Validator.class);
		String[] springValidatorBeans = this.context.getBeanNamesForType(Validator.class);
		assertThat(jsrValidatorBeans).containsExactly("defaultValidator");
		assertThat(springValidatorBeans).containsExactly("customValidator",
				"defaultValidator", "webFluxValidator");
		Validator validator = this.context.getBean("webFluxValidator", Validator.class);
		assertThat(validator).isInstanceOf(ValidatorAdapter.class);
		Object defaultValidator = this.context.getBean("defaultValidator");
		assertThat(((ValidatorAdapter) validator).getTarget()).isSameAs(defaultValidator);
		// Primary Spring validator is the one used by WebFlux behind the scenes
		assertThat(this.context.getBean(Validator.class)).isEqualTo(defaultValidator);
	}

	@Test
	public void validatorWithCustomJsr303ValidatorExposedAsSpringValidator() {
		load(CustomJsr303Validator.class);
		assertThat(this.context.getBeansOfType(ValidatorFactory.class)).isEmpty();
		String[] jsrValidatorBeans = this.context
				.getBeanNamesForType(javax.validation.Validator.class);
		String[] springValidatorBeans = this.context.getBeanNamesForType(Validator.class);
		assertThat(jsrValidatorBeans).containsExactly("customValidator");
		assertThat(springValidatorBeans).containsExactly("webFluxValidator");
		Validator validator = this.context.getBean(Validator.class);
		assertThat(validator).isInstanceOf(ValidatorAdapter.class);
		Validator target = ((ValidatorAdapter) validator).getTarget();
		assertThat(new DirectFieldAccessor(target).getPropertyValue("targetValidator"))
				.isSameAs(this.context.getBean("customValidator"));
	}

	private void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... environment) {
		load(config, null, environment);
	}

	private void load(Class<?> config, Class<?>[] exclude, String... environment) {
		this.context = new GenericReactiveWebApplicationContext();
		TestPropertyValues.of(environment).applyTo(this.context);
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
	protected static class CustomCodecCustomizers {

		@Bean
		public CodecCustomizer firstCodecCustomizer() {
			return mock(CodecCustomizer.class);
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
	@Import({ WebFluxAutoConfiguration.class })
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

		private final Validator validator = mock(Validator.class);

		@Override
		public Validator getValidator() {
			return this.validator;
		}

	}

	@Configuration
	protected static class ValidatorJsr303WebFluxConfigurer implements WebFluxConfigurer {

		private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

		@Override
		public Validator getValidator() {
			return this.validator;
		}

	}

	@Configuration
	static class CustomJsr303Validator {

		@Bean
		public javax.validation.Validator customValidator() {
			return mock(javax.validation.Validator.class);
		}

	}

	@Configuration
	static class CustomSpringValidator {

		@Bean
		public Validator customValidator() {
			return mock(Validator.class);
		}

	}

}
