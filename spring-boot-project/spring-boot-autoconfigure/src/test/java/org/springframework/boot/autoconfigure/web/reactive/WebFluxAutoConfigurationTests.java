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

package org.springframework.boot.autoconfigure.web.reactive;

import java.util.Date;
import java.util.List;

import javax.validation.ValidatorFactory;

import org.joda.time.DateTime;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidatorAdapter;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.boot.web.reactive.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.filter.reactive.HiddenHttpMethodFilter;
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
 * @author Artsiom Yudovin
 */
public class WebFluxAutoConfigurationTests {

	private static final MockReactiveWebServerFactory mockReactiveWebServerFactory = new MockReactiveWebServerFactory();

	private ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(WebFluxAutoConfiguration.class))
			.withUserConfiguration(Config.class);

	@Test
	public void shouldNotProcessIfExistingWebReactiveConfiguration() {
		this.contextRunner.withUserConfiguration(WebFluxConfigurationSupport.class).run((context) -> {
			assertThat(context).getBeans(RequestMappingHandlerMapping.class).hasSize(1);
			assertThat(context).getBeans(RequestMappingHandlerAdapter.class).hasSize(1);
		});
	}

	@Test
	public void shouldCreateDefaultBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).getBeans(RequestMappingHandlerMapping.class).hasSize(1);
			assertThat(context).getBeans(RequestMappingHandlerAdapter.class).hasSize(1);
			assertThat(context).getBeans(RequestedContentTypeResolver.class).hasSize(1);
			assertThat(context.getBean("resourceHandlerMapping", HandlerMapping.class)).isNotNull();
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldRegisterCustomHandlerMethodArgumentResolver() {
		this.contextRunner.withUserConfiguration(CustomArgumentResolvers.class).run((context) -> {
			RequestMappingHandlerAdapter adapter = context.getBean(RequestMappingHandlerAdapter.class);
			List<HandlerMethodArgumentResolver> customResolvers = (List<HandlerMethodArgumentResolver>) ReflectionTestUtils
					.getField(adapter.getArgumentResolverConfigurer(), "customResolvers");
			assertThat(customResolvers).contains(context.getBean("firstResolver", HandlerMethodArgumentResolver.class),
					context.getBean("secondResolver", HandlerMethodArgumentResolver.class));
		});
	}

	@Test
	public void shouldCustomizeCodecs() {
		this.contextRunner.withUserConfiguration(CustomCodecCustomizers.class).run((context) -> {
			CodecCustomizer codecCustomizer = context.getBean("firstCodecCustomizer", CodecCustomizer.class);
			assertThat(codecCustomizer).isNotNull();
			verify(codecCustomizer).customize(any(ServerCodecConfigurer.class));
		});
	}

	@Test
	public void shouldRegisterResourceHandlerMapping() {
		this.contextRunner.run((context) -> {
			SimpleUrlHandlerMapping hm = context.getBean("resourceHandlerMapping", SimpleUrlHandlerMapping.class);
			assertThat(hm.getUrlMap().get("/**")).isInstanceOf(ResourceWebHandler.class);
			ResourceWebHandler staticHandler = (ResourceWebHandler) hm.getUrlMap().get("/**");
			assertThat(staticHandler.getLocations()).hasSize(4);
			assertThat(hm.getUrlMap().get("/webjars/**")).isInstanceOf(ResourceWebHandler.class);
			ResourceWebHandler webjarsHandler = (ResourceWebHandler) hm.getUrlMap().get("/webjars/**");
			assertThat(webjarsHandler.getLocations()).hasSize(1);
			assertThat(webjarsHandler.getLocations().get(0))
					.isEqualTo(new ClassPathResource("/META-INF/resources/webjars/"));
		});
	}

	@Test
	public void shouldMapResourcesToCustomPath() {
		this.contextRunner.withPropertyValues("spring.webflux.static-path-pattern:/static/**").run((context) -> {
			SimpleUrlHandlerMapping hm = context.getBean("resourceHandlerMapping", SimpleUrlHandlerMapping.class);
			assertThat(hm.getUrlMap().get("/static/**")).isInstanceOf(ResourceWebHandler.class);
			ResourceWebHandler staticHandler = (ResourceWebHandler) hm.getUrlMap().get("/static/**");
			assertThat(staticHandler.getLocations()).hasSize(4);
		});
	}

	@Test
	public void shouldNotMapResourcesWhenDisabled() {
		this.contextRunner.withPropertyValues("spring.resources.add-mappings:false")
				.run((context) -> assertThat(context.getBean("resourceHandlerMapping"))
						.isNotInstanceOf(SimpleUrlHandlerMapping.class));
	}

	@Test
	public void resourceHandlerChainEnabled() {
		this.contextRunner.withPropertyValues("spring.resources.chain.enabled:true").run((context) -> {
			SimpleUrlHandlerMapping hm = context.getBean("resourceHandlerMapping", SimpleUrlHandlerMapping.class);
			assertThat(hm.getUrlMap().get("/**")).isInstanceOf(ResourceWebHandler.class);
			ResourceWebHandler staticHandler = (ResourceWebHandler) hm.getUrlMap().get("/**");
			assertThat(staticHandler.getResourceResolvers()).extractingResultOf("getClass")
					.containsOnly(CachingResourceResolver.class, PathResourceResolver.class);
			assertThat(staticHandler.getResourceTransformers()).extractingResultOf("getClass")
					.containsOnly(CachingResourceTransformer.class);
		});
	}

	@Test
	public void shouldRegisterViewResolvers() {
		this.contextRunner.withUserConfiguration(ViewResolvers.class).run((context) -> {
			ViewResolutionResultHandler resultHandler = context.getBean(ViewResolutionResultHandler.class);
			assertThat(resultHandler.getViewResolvers()).containsExactly(
					context.getBean("aViewResolver", ViewResolver.class),
					context.getBean("anotherViewResolver", ViewResolver.class));
		});
	}

	@Test
	public void noDateFormat() {
		this.contextRunner.run((context) -> {
			FormattingConversionService conversionService = context.getBean(FormattingConversionService.class);
			Date date = new DateTime(1988, 6, 25, 20, 30).toDate();
			// formatting conversion service should use simple toString()
			assertThat(conversionService.convert(date, String.class)).isEqualTo(date.toString());
		});
	}

	@Test
	public void overrideDateFormat() {
		this.contextRunner.withPropertyValues("spring.webflux.date-format:dd*MM*yyyy").run((context) -> {
			FormattingConversionService conversionService = context.getBean(FormattingConversionService.class);
			Date date = new DateTime(1988, 6, 25, 20, 30).toDate();
			assertThat(conversionService.convert(date, String.class)).isEqualTo("25*06*1988");
		});
	}

	@Test
	public void validatorWhenNoValidatorShouldUseDefault() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(ValidatorFactory.class);
			assertThat(context).doesNotHaveBean(javax.validation.Validator.class);
			assertThat(context).getBeanNames(Validator.class).containsExactly("webFluxValidator");
		});
	}

	@Test
	public void validatorWhenNoCustomizationShouldUseAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
				.run((context) -> {
					assertThat(context).getBeanNames(javax.validation.Validator.class)
							.containsExactly("defaultValidator");
					assertThat(context).getBeanNames(Validator.class).containsExactlyInAnyOrder("defaultValidator",
							"webFluxValidator");
					Validator validator = context.getBean("webFluxValidator", Validator.class);
					assertThat(validator).isInstanceOf(ValidatorAdapter.class);
					Object defaultValidator = context.getBean("defaultValidator");
					assertThat(((ValidatorAdapter) validator).getTarget()).isSameAs(defaultValidator);
					// Primary Spring validator is the one used by WebFlux behind the
					// scenes
					assertThat(context.getBean(Validator.class)).isEqualTo(defaultValidator);
				});
	}

	@Test
	public void validatorWithConfigurerShouldUseSpringValidator() {
		this.contextRunner.withUserConfiguration(ValidatorWebFluxConfigurer.class).run((context) -> {
			assertThat(context).doesNotHaveBean(ValidatorFactory.class);
			assertThat(context).doesNotHaveBean(javax.validation.Validator.class);
			assertThat(context).getBeanNames(Validator.class).containsOnly("webFluxValidator");
			assertThat(context.getBean("webFluxValidator"))
					.isSameAs(context.getBean(ValidatorWebFluxConfigurer.class).validator);
		});
	}

	@Test
	public void validatorWithConfigurerDoesNotExposeJsr303() {
		this.contextRunner.withUserConfiguration(ValidatorJsr303WebFluxConfigurer.class).run((context) -> {
			assertThat(context).doesNotHaveBean(ValidatorFactory.class);
			assertThat(context).doesNotHaveBean(javax.validation.Validator.class);
			assertThat(context).getBeanNames(Validator.class).containsOnly("webFluxValidator");
			Validator validator = context.getBean("webFluxValidator", Validator.class);
			assertThat(validator).isInstanceOf(ValidatorAdapter.class);
			assertThat(((ValidatorAdapter) validator).getTarget())
					.isSameAs(context.getBean(ValidatorJsr303WebFluxConfigurer.class).validator);
		});
	}

	@Test
	public void validationCustomConfigurerTakesPrecedence() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
				.withUserConfiguration(ValidatorWebFluxConfigurer.class).run((context) -> {
					assertThat(context).getBeans(ValidatorFactory.class).hasSize(1);
					assertThat(context).getBeans(javax.validation.Validator.class).hasSize(1);
					assertThat(context).getBeanNames(Validator.class).containsExactlyInAnyOrder("defaultValidator",
							"webFluxValidator");
					assertThat(context.getBean("webFluxValidator"))
							.isSameAs(context.getBean(ValidatorWebFluxConfigurer.class).validator);
					// Primary Spring validator is the auto-configured one as the WebFlux
					// one has been
					// customized via a WebFluxConfigurer
					assertThat(context.getBean(Validator.class)).isEqualTo(context.getBean("defaultValidator"));
				});
	}

	@Test
	public void validatorWithCustomSpringValidatorIgnored() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
				.withUserConfiguration(CustomSpringValidator.class).run((context) -> {
					assertThat(context).getBeanNames(javax.validation.Validator.class)
							.containsExactly("defaultValidator");
					assertThat(context).getBeanNames(Validator.class).containsExactlyInAnyOrder("customValidator",
							"defaultValidator", "webFluxValidator");
					Validator validator = context.getBean("webFluxValidator", Validator.class);
					assertThat(validator).isInstanceOf(ValidatorAdapter.class);
					Object defaultValidator = context.getBean("defaultValidator");
					assertThat(((ValidatorAdapter) validator).getTarget()).isSameAs(defaultValidator);
					// Primary Spring validator is the one used by WebFlux behind the
					// scenes
					assertThat(context.getBean(Validator.class)).isEqualTo(defaultValidator);
				});
	}

	@Test
	public void validatorWithCustomJsr303ValidatorExposedAsSpringValidator() {
		this.contextRunner.withUserConfiguration(CustomJsr303Validator.class).run((context) -> {
			assertThat(context).doesNotHaveBean(ValidatorFactory.class);
			assertThat(context).getBeanNames(javax.validation.Validator.class).containsExactly("customValidator");
			assertThat(context).getBeanNames(Validator.class).containsExactly("webFluxValidator");
			Validator validator = context.getBean(Validator.class);
			assertThat(validator).isInstanceOf(ValidatorAdapter.class);
			Validator target = ((ValidatorAdapter) validator).getTarget();
			assertThat(new DirectFieldAccessor(target).getPropertyValue("targetValidator"))
					.isSameAs(context.getBean("customValidator"));
		});
	}

	@Test
	public void hiddenHttpMethodFilterIsAutoConfigured() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(OrderedHiddenHttpMethodFilter.class));
	}

	@Test
	public void hiddenHttpMethodFilterCanBeOverridden() {
		this.contextRunner.withUserConfiguration(CustomHiddenHttpMethodFilter.class).run((context) -> {
			assertThat(context).doesNotHaveBean(OrderedHiddenHttpMethodFilter.class);
			assertThat(context).hasSingleBean(HiddenHttpMethodFilter.class);
		});
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
	protected static class Config {

		@Bean
		public MockReactiveWebServerFactory mockReactiveWebServerFactory() {
			return mockReactiveWebServerFactory;
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

	@Configuration
	static class CustomHiddenHttpMethodFilter {

		@Bean
		public HiddenHttpMethodFilter customHiddenHttpMethodFilter() {
			return mock(HiddenHttpMethodFilter.class);
		}

	}

}
