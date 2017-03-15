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

import java.util.Optional;

import javax.validation.ValidatorFactory;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.validation.SpringValidator;
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
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
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
 */
public class WebFluxAnnotationAutoConfigurationTests {

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

	@Test
	public void shouldRegisterCustomHandlerMethodArgumentResolver() throws Exception {
		load(CustomArgumentResolvers.class);
		RequestMappingHandlerAdapter adapter = this.context
				.getBean(RequestMappingHandlerAdapter.class);
		assertThat(adapter.getArgumentResolvers()).contains(
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
	public void validationNoJsr303ValidatorExposedByDefault() {
		load();
		assertThat(this.context.getBeansOfType(ValidatorFactory.class)).isEmpty();
		assertThat(this.context.getBeansOfType(javax.validation.Validator.class))
				.isEmpty();
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(1);
	}

	@Test
	public void validationCustomConfigurerTakesPrecedence() {
		load(WebFluxValidator.class);
		assertThat(this.context.getBeansOfType(ValidatorFactory.class)).isEmpty();
		assertThat(this.context.getBeansOfType(javax.validation.Validator.class))
				.isEmpty();
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(1);
		Validator validator = this.context.getBean(Validator.class);
		assertThat(validator)
				.isSameAs(this.context.getBean(WebFluxValidator.class).validator);
	}

	@Test
	public void validationCustomConfigurerTakesPrecedenceAndDoNotExposeJsr303() {
		load(WebFluxJsr303Validator.class);
		assertThat(this.context.getBeansOfType(ValidatorFactory.class)).isEmpty();
		assertThat(this.context.getBeansOfType(javax.validation.Validator.class))
				.isEmpty();
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(1);
		Validator validator = this.context.getBean(Validator.class);
		assertThat(validator).isInstanceOf(SpringValidator.class);
		assertThat(((SpringValidator) validator).getTarget())
				.isSameAs(this.context.getBean(WebFluxJsr303Validator.class).validator);
	}

	@Test
	public void validationJsr303CustomValidatorReusedAsSpringValidator() {
		load(CustomValidator.class);
		assertThat(this.context.getBeansOfType(ValidatorFactory.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(javax.validation.Validator.class))
				.hasSize(1);
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(2);
		Validator validator = this.context.getBean("webFluxValidator", Validator.class);
		assertThat(validator).isInstanceOf(SpringValidator.class);
		assertThat(((SpringValidator) validator).getTarget())
				.isSameAs(this.context.getBean(javax.validation.Validator.class));
	}

	@Test
	public void validationJsr303ValidatorExposedAsSpringValidator() {
		load(Jsr303Validator.class);
		assertThat(this.context.getBeansOfType(ValidatorFactory.class)).isEmpty();
		assertThat(this.context.getBeansOfType(javax.validation.Validator.class))
				.hasSize(1);
		assertThat(this.context.getBeansOfType(Validator.class)).hasSize(1);
		Validator validator = this.context.getBean(Validator.class);
		assertThat(validator).isInstanceOf(SpringValidator.class);
		SpringValidatorAdapter target = ((SpringValidator) validator).getTarget();
		assertThat(new DirectFieldAccessor(target).getPropertyValue("targetValidator"))
				.isSameAs(this.context.getBean(javax.validation.Validator.class));
	}

	private void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... environment) {
		this.context = new GenericReactiveWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, environment);
		if (config != null) {
			this.context.register(config);
		}
		this.context.register(BaseConfiguration.class);
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
	protected static class WebFluxValidator implements WebFluxConfigurer {

		private final Validator validator = mock(Validator.class);

		@Override
		public Optional<Validator> getValidator() {
			return Optional.of(this.validator);
		}

	}

	@Configuration
	protected static class WebFluxJsr303Validator implements WebFluxConfigurer {

		private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

		@Override
		public Optional<Validator> getValidator() {
			return Optional.of(this.validator);
		}

	}

	@Configuration
	static class Jsr303Validator {

		@Bean
		public javax.validation.Validator jsr303Validator() {
			return mock(javax.validation.Validator.class);
		}

	}

	@Configuration
	static class CustomValidator {

		@Bean
		public Validator customValidator() {
			return new LocalValidatorFactoryBean();
		}

	}

}
