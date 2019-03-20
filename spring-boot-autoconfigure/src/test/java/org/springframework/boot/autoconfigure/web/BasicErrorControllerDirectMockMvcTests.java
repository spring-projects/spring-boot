/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.ServletException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.util.ApplicationContextTestUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link BasicErrorController} using {@link MockMvc} but not
 * {@link SpringRunner}.
 *
 * @author Dave Syer
 * @author Sebastien Deleuze
 */
public class BasicErrorControllerDirectMockMvcTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private ConfigurableWebApplicationContext wac;

	private MockMvc mockMvc;

	@After
	public void close() {
		ApplicationContextTestUtils.closeAll(this.wac);
	}

	public void setup(ConfigurableWebApplicationContext context) {
		this.wac = context;
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test
	public void errorPageAvailableWithParentContext() throws Exception {
		setup((ConfigurableWebApplicationContext) new SpringApplicationBuilder(
				ParentConfiguration.class).child(ChildConfiguration.class)
						.run("--server.port=0"));
		MvcResult response = this.mockMvc
				.perform(get("/error").accept(MediaType.TEXT_HTML))
				.andExpect(status().is5xxServerError()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertThat(content).contains("status=999");
	}

	@Test
	public void errorPageAvailableWithMvcIncluded() throws Exception {
		setup((ConfigurableWebApplicationContext) new SpringApplication(
				WebMvcIncludedConfiguration.class).run("--server.port=0"));
		MvcResult response = this.mockMvc
				.perform(get("/error").accept(MediaType.TEXT_HTML))
				.andExpect(status().is5xxServerError()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertThat(content).contains("status=999");
	}

	@Test
	public void errorPageNotAvailableWithWhitelabelDisabled() throws Exception {
		setup((ConfigurableWebApplicationContext) new SpringApplication(
				WebMvcIncludedConfiguration.class).run("--server.port=0",
						"--server.error.whitelabel.enabled=false"));
		this.thrown.expect(ServletException.class);
		this.mockMvc.perform(get("/error").accept(MediaType.TEXT_HTML));
	}

	@Test
	public void errorControllerWithAop() throws Exception {
		setup((ConfigurableWebApplicationContext) new SpringApplication(
				WithAopConfiguration.class).run("--server.port=0"));
		MvcResult response = this.mockMvc
				.perform(get("/error").accept(MediaType.TEXT_HTML))
				.andExpect(status().is5xxServerError()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertThat(content).contains("status=999");
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import({ EmbeddedServletContainerAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, ErrorMvcAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	protected @interface MinimalWebConfiguration {

	}

	@Configuration
	@MinimalWebConfiguration
	protected static class ParentConfiguration {

	}

	@Configuration
	@MinimalWebConfiguration
	@EnableWebMvc
	protected static class WebMvcIncludedConfiguration {

		// For manual testing
		public static void main(String[] args) {
			SpringApplication.run(WebMvcIncludedConfiguration.class, args);
		}

	}

	@Configuration
	@MinimalWebConfiguration
	protected static class VanillaConfiguration {

		// For manual testing
		public static void main(String[] args) {
			SpringApplication.run(VanillaConfiguration.class, args);
		}

	}

	@Configuration
	@MinimalWebConfiguration
	protected static class ChildConfiguration {

		// For manual testing
		public static void main(String[] args) {
			new SpringApplicationBuilder(ParentConfiguration.class)
					.child(ChildConfiguration.class).run(args);
		}

	}

	@Configuration
	@EnableAspectJAutoProxy(proxyTargetClass = false)
	@MinimalWebConfiguration
	@Aspect
	protected static class WithAopConfiguration {

		@Pointcut("within(@org.springframework.stereotype.Controller *)")
		private void controllerPointCut() {
		};

		@Around("controllerPointCut()")
		public Object mvcAdvice(ProceedingJoinPoint pjp) throws Throwable {
			return pjp.proceed();
		}

	}

}
