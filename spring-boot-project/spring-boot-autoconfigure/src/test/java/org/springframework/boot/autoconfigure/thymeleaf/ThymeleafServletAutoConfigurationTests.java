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

package org.springframework.boot.autoconfigure.thymeleaf;

import java.io.File;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

import javax.servlet.DispatcherType;

import nz.net.ultraq.thymeleaf.LayoutDialect;
import nz.net.ultraq.thymeleaf.decorators.strategies.GroupingStrategy;
import org.junit.Rule;
import org.junit.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.ThymeleafView;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.filter.OrderedCharacterEncodingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;
import org.springframework.web.servlet.support.RequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * Tests for {@link ThymeleafAutoConfiguration} in Servlet-based applications.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Brian Clozel
 * @author Kazuki Shimizu
 * @author Artsiom Yudovin
 */
public class ThymeleafServletAutoConfigurationTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	private final BuildOutput buildOutput = new BuildOutput(getClass());

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ThymeleafAutoConfiguration.class));

	@Test
	public void autoConfigurationBackOffWithoutThymeleafSpring() {
		this.contextRunner
				.withClassLoader(new FilteredClassLoader("org.thymeleaf.spring5"))
				.run((context) -> assertThat(context)
						.doesNotHaveBean(TemplateEngine.class));
	}

	@Test
	public void createFromConfigClass() {
		this.contextRunner.withPropertyValues("spring.thymeleaf.mode:HTML",
				"spring.thymeleaf.suffix:").run((context) -> {
					assertThat(context).hasSingleBean(TemplateEngine.class);
					TemplateEngine engine = context.getBean(TemplateEngine.class);
					Context attrs = new Context(Locale.UK,
							Collections.singletonMap("foo", "bar"));
					String result = engine.process("template.html", attrs);
					assertThat(result).isEqualTo("<html>bar</html>");
				});
	}

	@Test
	public void overrideCharacterEncoding() {
		this.contextRunner.withPropertyValues("spring.thymeleaf.encoding:UTF-16")
				.run((context) -> {
					ITemplateResolver resolver = context.getBean(ITemplateResolver.class);
					assertThat(resolver)
							.isInstanceOf(SpringResourceTemplateResolver.class);
					assertThat(((SpringResourceTemplateResolver) resolver)
							.getCharacterEncoding()).isEqualTo("UTF-16");
					ThymeleafViewResolver views = context
							.getBean(ThymeleafViewResolver.class);
					assertThat(views.getCharacterEncoding()).isEqualTo("UTF-16");
					assertThat(views.getContentType())
							.isEqualTo("text/html;charset=UTF-16");
				});
	}

	@Test
	public void overrideDisableProducePartialOutputWhileProcessing() {
		this.contextRunner.withPropertyValues(
				"spring.thymeleaf.servlet.produce-partial-output-while-processing:false")
				.run((context) -> assertThat(context.getBean(ThymeleafViewResolver.class)
						.getProducePartialOutputWhileProcessing()).isFalse());
	}

	@Test
	public void disableProducePartialOutputWhileProcessingIsEnabledByDefault() {
		this.contextRunner
				.run((context) -> assertThat(context.getBean(ThymeleafViewResolver.class)
						.getProducePartialOutputWhileProcessing()).isTrue());
	}

	@Test
	public void overrideTemplateResolverOrder() {
		this.contextRunner.withPropertyValues("spring.thymeleaf.templateResolverOrder:25")
				.run((context) -> assertThat(
						context.getBean(ITemplateResolver.class).getOrder())
								.isEqualTo(Integer.valueOf(25)));
	}

	@Test
	public void overrideViewNames() {
		this.contextRunner.withPropertyValues("spring.thymeleaf.viewNames:foo,bar")
				.run((context) -> assertThat(
						context.getBean(ThymeleafViewResolver.class).getViewNames())
								.isEqualTo(new String[] { "foo", "bar" }));
	}

	@Test
	public void overrideEnableSpringElCompiler() {
		this.contextRunner
				.withPropertyValues("spring.thymeleaf.enable-spring-el-compiler:true")
				.run((context) -> assertThat(context.getBean(SpringTemplateEngine.class)
						.getEnableSpringELCompiler()).isTrue());
	}

	@Test
	public void enableSpringElCompilerIsDisabledByDefault() {
		this.contextRunner.run((context) -> assertThat(
				context.getBean(SpringTemplateEngine.class).getEnableSpringELCompiler())
						.isFalse());
	}

	@Test
	public void overrideRenderHiddenMarkersBeforeCheckboxes() {
		this.contextRunner
				.withPropertyValues(
						"spring.thymeleaf.render-hidden-markers-before-checkboxes:true")
				.run((context) -> assertThat(context.getBean(SpringTemplateEngine.class)
						.getRenderHiddenMarkersBeforeCheckboxes()).isTrue());
	}

	@Test
	public void enableRenderHiddenMarkersBeforeCheckboxesIsDisabledByDefault() {
		this.contextRunner
				.run((context) -> assertThat(context.getBean(SpringTemplateEngine.class)
						.getRenderHiddenMarkersBeforeCheckboxes()).isFalse());
	}

	@Test
	public void templateLocationDoesNotExist() {
		this.contextRunner
				.withPropertyValues(
						"spring.thymeleaf.prefix:classpath:/no-such-directory/")
				.run((context) -> this.output
						.expect(containsString("Cannot find template location")));
	}

	@Test
	public void templateLocationEmpty() {
		new File(this.buildOutput.getTestResourcesLocation(),
				"empty-templates/empty-directory").mkdirs();
		this.contextRunner.withPropertyValues(
				"spring.thymeleaf.prefix:classpath:/empty-templates/empty-directory/")
				.run((context) -> this.output
						.expect(not(containsString("Cannot find template location"))));
	}

	@Test
	public void createLayoutFromConfigClass() {
		this.contextRunner.run((context) -> {
			ThymeleafView view = (ThymeleafView) context
					.getBean(ThymeleafViewResolver.class)
					.resolveViewName("view", Locale.UK);
			MockHttpServletResponse response = new MockHttpServletResponse();
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.setAttribute(RequestContext.WEB_APPLICATION_CONTEXT_ATTRIBUTE,
					context);
			view.render(Collections.singletonMap("foo", "bar"), request, response);
			String result = response.getContentAsString();
			assertThat(result).contains("<title>Content</title>");
			assertThat(result).contains("<span>bar</span>");
			context.close();
		});
	}

	@Test
	public void useDataDialect() {
		this.contextRunner.run((context) -> {
			TemplateEngine engine = context.getBean(TemplateEngine.class);
			Context attrs = new Context(Locale.UK,
					Collections.singletonMap("foo", "bar"));
			String result = engine.process("data-dialect", attrs);
			assertThat(result).isEqualTo("<html><body data-foo=\"bar\"></body></html>");
		});
	}

	@Test
	public void useJava8TimeDialect() {
		this.contextRunner.run((context) -> {
			TemplateEngine engine = context.getBean(TemplateEngine.class);
			Context attrs = new Context(Locale.UK);
			String result = engine.process("java8time-dialect", attrs);
			assertThat(result).isEqualTo("<html><body>2015-11-24</body></html>");
		});
	}

	@Test
	public void useSecurityDialect() {
		this.contextRunner.run((context) -> {
			TemplateEngine engine = context.getBean(TemplateEngine.class);
			WebContext attrs = new WebContext(new MockHttpServletRequest(),
					new MockHttpServletResponse(), new MockServletContext());
			try {
				SecurityContextHolder.setContext(new SecurityContextImpl(
						new TestingAuthenticationToken("alice", "admin")));
				String result = engine.process("security-dialect", attrs);
				assertThat(result).isEqualTo("<html><body><div>alice</div></body></html>"
						+ System.lineSeparator());
			}
			finally {
				SecurityContextHolder.clearContext();
			}
		});
	}

	@Test
	public void renderTemplate() {
		this.contextRunner.run((context) -> {
			TemplateEngine engine = context.getBean(TemplateEngine.class);
			Context attrs = new Context(Locale.UK,
					Collections.singletonMap("foo", "bar"));
			String result = engine.process("home", attrs);
			assertThat(result).isEqualTo("<html><body>bar</body></html>");
		});
	}

	@Test
	public void renderNonWebAppTemplate() {
		new ApplicationContextRunner()
				.withConfiguration(
						AutoConfigurations.of(ThymeleafAutoConfiguration.class))
				.run((context) -> {
					assertThat(context).doesNotHaveBean(ViewResolver.class);
					TemplateEngine engine = context.getBean(TemplateEngine.class);
					Context attrs = new Context(Locale.UK,
							Collections.singletonMap("greeting", "Hello World"));
					String result = engine.process("message", attrs);
					assertThat(result).contains("Hello World");
				});
	}

	@Test
	public void registerResourceHandlingFilterDisabledByDefault() {
		this.contextRunner.run((context) -> assertThat(context)
				.doesNotHaveBean(FilterRegistrationBean.class));
	}

	@Test
	public void registerResourceHandlingFilterOnlyIfResourceChainIsEnabled() {
		this.contextRunner.withPropertyValues("spring.resources.chain.enabled:true")
				.run((context) -> {
					FilterRegistrationBean<?> registration = context
							.getBean(FilterRegistrationBean.class);
					assertThat(registration.getFilter())
							.isInstanceOf(ResourceUrlEncodingFilter.class);
					assertThat(registration).hasFieldOrPropertyWithValue(
							"dispatcherTypes",
							EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
				});
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void registerResourceHandlingFilterWithOtherRegistrationBean() {
		// gh-14897
		this.contextRunner
				.withUserConfiguration(FilterRegistrationOtherConfiguration.class)
				.withPropertyValues("spring.resources.chain.enabled:true")
				.run((context) -> {
					Map<String, FilterRegistrationBean> beans = context
							.getBeansOfType(FilterRegistrationBean.class);
					assertThat(beans).hasSize(2);
					FilterRegistrationBean registration = beans.values().stream().filter(
							(r) -> r.getFilter() instanceof ResourceUrlEncodingFilter)
							.findFirst().get();
					assertThat(registration).hasFieldOrPropertyWithValue(
							"dispatcherTypes",
							EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
				});
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void registerResourceHandlingFilterWithResourceRegistrationBean() {
		// gh-14926
		this.contextRunner
				.withUserConfiguration(FilterRegistrationResourceConfiguration.class)
				.withPropertyValues("spring.resources.chain.enabled:true")
				.run((context) -> {
					Map<String, FilterRegistrationBean> beans = context
							.getBeansOfType(FilterRegistrationBean.class);
					assertThat(beans).hasSize(1);
					FilterRegistrationBean registration = beans.values().stream().filter(
							(r) -> r.getFilter() instanceof ResourceUrlEncodingFilter)
							.findFirst().get();
					assertThat(registration).hasFieldOrPropertyWithValue(
							"dispatcherTypes", EnumSet.of(DispatcherType.INCLUDE));
				});
	}

	@Test
	public void layoutDialectCanBeCustomized() {
		this.contextRunner.withUserConfiguration(LayoutDialectConfiguration.class)
				.run((context) -> assertThat(ReflectionTestUtils.getField(
						context.getBean(LayoutDialect.class), "sortingStrategy"))
								.isInstanceOf(GroupingStrategy.class));
	}

	@Test
	public void cachingCanBeDisabled() {
		this.contextRunner.withPropertyValues("spring.thymeleaf.cache:false")
				.run((context) -> {
					assertThat(context.getBean(ThymeleafViewResolver.class).isCache())
							.isFalse();
					SpringResourceTemplateResolver templateResolver = context
							.getBean(SpringResourceTemplateResolver.class);
					assertThat(templateResolver.isCacheable()).isFalse();
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class LayoutDialectConfiguration {

		@Bean
		public LayoutDialect layoutDialect() {
			return new LayoutDialect(new GroupingStrategy());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FilterRegistrationResourceConfiguration {

		@Bean
		public FilterRegistrationBean<ResourceUrlEncodingFilter> filterRegistration() {
			FilterRegistrationBean<ResourceUrlEncodingFilter> bean = new FilterRegistrationBean<>(
					new ResourceUrlEncodingFilter());
			bean.setDispatcherTypes(EnumSet.of(DispatcherType.INCLUDE));
			return bean;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FilterRegistrationOtherConfiguration {

		@Bean
		public FilterRegistrationBean<OrderedCharacterEncodingFilter> filterRegistration() {
			return new FilterRegistrationBean<>(new OrderedCharacterEncodingFilter());
		}

	}

}
