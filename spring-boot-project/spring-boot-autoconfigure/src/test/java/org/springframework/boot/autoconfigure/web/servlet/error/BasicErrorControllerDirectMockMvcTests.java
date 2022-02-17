/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet.error;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.servlet.ServletException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.util.ApplicationContextTestUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link BasicErrorController} using {@link MockMvc} but not
 * {@link org.springframework.test.context.junit.jupiter.SpringExtension}.
 *
 * @author Dave Syer
 * @author Sebastien Deleuze
 */
class BasicErrorControllerDirectMockMvcTests {

	private ConfigurableWebApplicationContext wac;

	private MockMvc mockMvc;

	@AfterEach
	void close() {
		ApplicationContextTestUtils.closeAll(this.wac);
	}

	void setup(ConfigurableWebApplicationContext context) {
		this.wac = context;
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test
	void errorPageAvailableWithParentContext() throws Exception {
		setup((ConfigurableWebApplicationContext) new SpringApplicationBuilder(ParentConfiguration.class)
				.child(ChildConfiguration.class).run("--server.port=0"));
		MvcResult response = this.mockMvc.perform(get("/error").accept(MediaType.TEXT_HTML))
				.andExpect(status().is5xxServerError()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertThat(content).contains("status=999");
	}

	@Test
	void errorPageAvailableWithMvcIncluded() throws Exception {
		setup((ConfigurableWebApplicationContext) new SpringApplication(WebMvcIncludedConfiguration.class)
				.run("--server.port=0"));
		MvcResult response = this.mockMvc.perform(get("/error").accept(MediaType.TEXT_HTML))
				.andExpect(status().is5xxServerError()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertThat(content).contains("status=999");
	}

	@Test
	void errorPageNotAvailableWithWhitelabelDisabled() {
		setup((ConfigurableWebApplicationContext) new SpringApplication(WebMvcIncludedConfiguration.class)
				.run("--server.port=0", "--server.error.whitelabel.enabled=false"));
		assertThatExceptionOfType(ServletException.class)
				.isThrownBy(() -> this.mockMvc.perform(get("/error").accept(MediaType.TEXT_HTML)));
	}

	@Test
	void errorControllerWithAop() throws Exception {
		setup((ConfigurableWebApplicationContext) new SpringApplication(WithAopConfiguration.class)
				.run("--server.port=0"));
		MvcResult response = this.mockMvc.perform(get("/error").accept(MediaType.TEXT_HTML))
				.andExpect(status().is5xxServerError()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertThat(content).contains("status=999");
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import({ ServletWebServerFactoryAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
			WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			ErrorMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected @interface MinimalWebConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@MinimalWebConfiguration
	static class ParentConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@MinimalWebConfiguration
	@EnableWebMvc
	static class WebMvcIncludedConfiguration {

		// For manual testing
		static void main(String[] args) {
			SpringApplication.run(WebMvcIncludedConfiguration.class, args);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@MinimalWebConfiguration
	static class VanillaConfiguration {

		// For manual testing
		static void main(String[] args) {
			SpringApplication.run(VanillaConfiguration.class, args);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@MinimalWebConfiguration
	static class ChildConfiguration {

		// For manual testing
		static void main(String[] args) {
			new SpringApplicationBuilder(ParentConfiguration.class).child(ChildConfiguration.class).run(args);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAspectJAutoProxy(proxyTargetClass = false)
	@MinimalWebConfiguration
	@Aspect
	static class WithAopConfiguration {

		@Pointcut("within(@org.springframework.stereotype.Controller *)")
		private void controllerPointCut() {
		}

		@Around("controllerPointCut()")
		Object mvcAdvice(ProceedingJoinPoint pjp) throws Throwable {
			return pjp.proceed();
		}

	}

}
