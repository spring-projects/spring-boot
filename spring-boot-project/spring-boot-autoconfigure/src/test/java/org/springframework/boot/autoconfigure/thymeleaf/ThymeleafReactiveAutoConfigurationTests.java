/*
 * Copyright 2012-2020 the original author or authors.
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
import java.util.Locale;

import nz.net.ultraq.thymeleaf.LayoutDialect;
import nz.net.ultraq.thymeleaf.decorators.strategies.GroupingRespectLayoutTitleStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.extras.springsecurity5.util.SpringSecurityContextUtils;
import org.thymeleaf.spring5.ISpringWebFluxTemplateEngine;
import org.thymeleaf.spring5.SpringWebFluxTemplateEngine;
import org.thymeleaf.spring5.context.webflux.SpringWebFluxContext;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.reactive.ThymeleafReactiveViewResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ThymeleafAutoConfiguration} in Reactive applications.
 *
 * @author Brian Clozel
 * @author Kazuki Shimizu
 * @author Stephane Nicoll
 */
@ExtendWith(OutputCaptureExtension.class)
class ThymeleafReactiveAutoConfigurationTests {

	private final BuildOutput buildOutput = new BuildOutput(getClass());

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ThymeleafAutoConfiguration.class));

	@Test
	void createFromConfigClass() {
		this.contextRunner.withPropertyValues("spring.thymeleaf.suffix:.html").run((context) -> {
			TemplateEngine engine = context.getBean(TemplateEngine.class);
			Context attrs = new Context(Locale.UK, Collections.singletonMap("foo", "bar"));
			String result = engine.process("template", attrs).trim();
			assertThat(result).isEqualTo("<html>bar</html>");
		});
	}

	@Test
	void overrideCharacterEncoding() {
		this.contextRunner.withPropertyValues("spring.thymeleaf.encoding:UTF-16").run((context) -> {
			ITemplateResolver resolver = context.getBean(ITemplateResolver.class);
			assertThat(resolver).isInstanceOf(SpringResourceTemplateResolver.class);
			assertThat(((SpringResourceTemplateResolver) resolver).getCharacterEncoding()).isEqualTo("UTF-16");
			ThymeleafReactiveViewResolver views = context.getBean(ThymeleafReactiveViewResolver.class);
			assertThat(views.getDefaultCharset().name()).isEqualTo("UTF-16");
		});
	}

	@Test
	void overrideMediaTypes() {
		this.contextRunner.withPropertyValues("spring.thymeleaf.reactive.media-types:text/html,text/plain").run(
				(context) -> assertThat(context.getBean(ThymeleafReactiveViewResolver.class).getSupportedMediaTypes())
						.contains(MediaType.TEXT_HTML, MediaType.TEXT_PLAIN));
	}

	@Test
	void overrideTemplateResolverOrder() {
		this.contextRunner.withPropertyValues("spring.thymeleaf.templateResolverOrder:25")
				.run((context) -> assertThat(context.getBean(ITemplateResolver.class).getOrder())
						.isEqualTo(Integer.valueOf(25)));
	}

	@Test
	void overrideViewNames() {
		this.contextRunner.withPropertyValues("spring.thymeleaf.viewNames:foo,bar")
				.run((context) -> assertThat(context.getBean(ThymeleafReactiveViewResolver.class).getViewNames())
						.isEqualTo(new String[] { "foo", "bar" }));
	}

	@Test
	void overrideMaxChunkSize() {
		this.contextRunner.withPropertyValues("spring.thymeleaf.reactive.maxChunkSize:8KB")
				.run((context) -> assertThat(
						context.getBean(ThymeleafReactiveViewResolver.class).getResponseMaxChunkSizeBytes())
								.isEqualTo(Integer.valueOf(8192)));
	}

	@Test
	void overrideFullModeViewNames() {
		this.contextRunner.withPropertyValues("spring.thymeleaf.reactive.fullModeViewNames:foo,bar").run(
				(context) -> assertThat(context.getBean(ThymeleafReactiveViewResolver.class).getFullModeViewNames())
						.isEqualTo(new String[] { "foo", "bar" }));
	}

	@Test
	void overrideChunkedModeViewNames() {
		this.contextRunner.withPropertyValues("spring.thymeleaf.reactive.chunkedModeViewNames:foo,bar").run(
				(context) -> assertThat(context.getBean(ThymeleafReactiveViewResolver.class).getChunkedModeViewNames())
						.isEqualTo(new String[] { "foo", "bar" }));
	}

	@Test
	void overrideEnableSpringElCompiler() {
		this.contextRunner.withPropertyValues("spring.thymeleaf.enable-spring-el-compiler:true").run(
				(context) -> assertThat(context.getBean(SpringWebFluxTemplateEngine.class).getEnableSpringELCompiler())
						.isTrue());
	}

	@Test
	void enableSpringElCompilerIsDisabledByDefault() {
		this.contextRunner.run(
				(context) -> assertThat(context.getBean(SpringWebFluxTemplateEngine.class).getEnableSpringELCompiler())
						.isFalse());
	}

	@Test
	void overrideRenderHiddenMarkersBeforeCheckboxes() {
		this.contextRunner.withPropertyValues("spring.thymeleaf.render-hidden-markers-before-checkboxes:true")
				.run((context) -> assertThat(
						context.getBean(SpringWebFluxTemplateEngine.class).getRenderHiddenMarkersBeforeCheckboxes())
								.isTrue());
	}

	@Test
	void enableRenderHiddenMarkersBeforeCheckboxesIsDisabledByDefault() {
		this.contextRunner.run((context) -> assertThat(
				context.getBean(SpringWebFluxTemplateEngine.class).getRenderHiddenMarkersBeforeCheckboxes()).isFalse());
	}

	@Test
	void templateLocationDoesNotExist(CapturedOutput output) {
		this.contextRunner.withPropertyValues("spring.thymeleaf.prefix:classpath:/no-such-directory/")
				.run((context) -> assertThat(output).contains("Cannot find template location"));
	}

	@Test
	void templateLocationEmpty(CapturedOutput output) {
		new File(this.buildOutput.getTestResourcesLocation(), "empty-templates/empty-directory").mkdirs();
		this.contextRunner.withPropertyValues("spring.thymeleaf.prefix:classpath:/empty-templates/empty-directory/")
				.run((context) -> assertThat(output).doesNotContain("Cannot find template location"));
	}

	@Test
	void useDataDialect() {
		this.contextRunner.run((context) -> {
			ISpringWebFluxTemplateEngine engine = context.getBean(ISpringWebFluxTemplateEngine.class);
			Context attrs = new Context(Locale.UK, Collections.singletonMap("foo", "bar"));
			String result = engine.process("data-dialect", attrs).trim();
			assertThat(result).isEqualTo("<html><body data-foo=\"bar\"></body></html>");
		});
	}

	@Test
	void useJava8TimeDialect() {
		this.contextRunner.run((context) -> {
			ISpringWebFluxTemplateEngine engine = context.getBean(ISpringWebFluxTemplateEngine.class);
			Context attrs = new Context(Locale.UK);
			String result = engine.process("java8time-dialect", attrs).trim();
			assertThat(result).isEqualTo("<html><body>2015-11-24</body></html>");
		});
	}

	@Test
	void useSecurityDialect() {
		this.contextRunner.run((context) -> {
			ISpringWebFluxTemplateEngine engine = context.getBean(ISpringWebFluxTemplateEngine.class);
			MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());
			exchange.getAttributes().put(SpringSecurityContextUtils.SECURITY_CONTEXT_MODEL_ATTRIBUTE_NAME,
					new SecurityContextImpl(new TestingAuthenticationToken("alice", "admin")));
			IContext attrs = new SpringWebFluxContext(exchange);
			String result = engine.process("security-dialect", attrs);
			assertThat(result).isEqualTo("<html><body><div>alice</div></body></html>" + System.lineSeparator());
		});
	}

	@Test
	void renderTemplate() {
		this.contextRunner.run((context) -> {
			ISpringWebFluxTemplateEngine engine = context.getBean(ISpringWebFluxTemplateEngine.class);
			Context attrs = new Context(Locale.UK, Collections.singletonMap("foo", "bar"));
			String result = engine.process("home", attrs).trim();
			assertThat(result).isEqualTo("<html><body>bar</body></html>");
		});
	}

	@Test
	void layoutDialectCanBeCustomized() {
		this.contextRunner.withUserConfiguration(LayoutDialectConfiguration.class)
				.run((context) -> assertThat(
						ReflectionTestUtils.getField(context.getBean(LayoutDialect.class), "sortingStrategy"))
								.isInstanceOf(GroupingRespectLayoutTitleStrategy.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class LayoutDialectConfiguration {

		@Bean
		LayoutDialect layoutDialect() {
			return new LayoutDialect(new GroupingRespectLayoutTitleStrategy());
		}

	}

}
