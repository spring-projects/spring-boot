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

package org.springframework.boot.grpc.server.autoconfigure.security;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.grpc.server.GrpcServletRegistration;
import org.springframework.boot.grpc.server.autoconfigure.security.GrpcDisableCsrfHttpConfigurer.GrpcCsrfRequestMatcher;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link GrpcDisableCsrfHttpConfigurer}.
 *
 * @author Phillip Webb
 */
class GrpcDisableCsrfHttpConfigurerTests {

	private GrpcDisableCsrfHttpConfigurer configurer = new GrpcDisableCsrfHttpConfigurer();

	@Test
	void initDisablesCsrf() {
		ObjectPostProcessor<Object> objectPostProcessor = ObjectPostProcessor.identity();
		AuthenticationManagerBuilder authenticationBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
		HttpSecurity http = new HttpSecurity(objectPostProcessor, authenticationBuilder, new HashMap<>());
		StaticApplicationContext applicationContext = addApplicationContext(http);
		addServiceDiscover(applicationContext);
		addGrpcServletRegistration(applicationContext);
		CsrfConfigurer<?> csrf = addCsrf(http);
		this.configurer.init(http);
		ArgumentCaptor<RequestMatcher> matcher = ArgumentCaptor.captor();
		then(csrf).should().requireCsrfProtectionMatcher(matcher.capture());
		assertThat(matcher.getValue()).isSameAs(GrpcCsrfRequestMatcher.INSTANCE);
	}

	@Test
	void initWhenNoApplicationContextDoesNothing() {
		ObjectPostProcessor<Object> objectPostProcessor = ObjectPostProcessor.identity();
		AuthenticationManagerBuilder authenticationBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
		HttpSecurity http = new HttpSecurity(objectPostProcessor, authenticationBuilder, new HashMap<>());
		CsrfConfigurer<?> csrf = addCsrf(http);
		this.configurer.init(http);
		then(csrf).should(never()).requireCsrfProtectionMatcher(any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void initWhenNoCsrfConfigurerDoesNothing() {
		ObjectPostProcessor<Object> objectPostProcessor = ObjectPostProcessor.identity();
		AuthenticationManagerBuilder authenticationBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
		HttpSecurity http = new HttpSecurity(objectPostProcessor, authenticationBuilder, new HashMap<>());
		StaticApplicationContext applicationContext = addApplicationContext(http);
		addServiceDiscover(applicationContext);
		addGrpcServletRegistration(applicationContext);
		this.configurer.init(http);
		CsrfConfigurer<?> csrfConfigurer = http.getConfigurer(CsrfConfigurer.class);
		assertThat(csrfConfigurer).isNull();
	}

	@Test
	void initWhenNoGrpcServiceDiscovererBeanDoesNothing() {
		ObjectPostProcessor<Object> objectPostProcessor = ObjectPostProcessor.identity();
		AuthenticationManagerBuilder authenticationBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
		HttpSecurity http = new HttpSecurity(objectPostProcessor, authenticationBuilder, new HashMap<>());
		StaticApplicationContext applicationContext = addApplicationContext(http);
		addGrpcServletRegistration(applicationContext);
		CsrfConfigurer<?> csrf = addCsrf(http);
		this.configurer.init(http);
		then(csrf).should(never()).requireCsrfProtectionMatcher(any());
	}

	@Test
	void initWhenNoGrpcServletRegistrationBeanDoesNothing() {
		ObjectPostProcessor<Object> objectPostProcessor = ObjectPostProcessor.identity();
		AuthenticationManagerBuilder authenticationBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
		HttpSecurity http = new HttpSecurity(objectPostProcessor, authenticationBuilder, new HashMap<>());
		StaticApplicationContext applicationContext = addApplicationContext(http);
		addServiceDiscover(applicationContext);
		CsrfConfigurer<?> csrf = addCsrf(http);
		this.configurer.init(http);
		then(csrf).should(never()).requireCsrfProtectionMatcher(any());
	}

	@Test
	void initWhenEnabledPropertyFalseDoesNothing() {
		ObjectPostProcessor<Object> objectPostProcessor = ObjectPostProcessor.identity();
		AuthenticationManagerBuilder authenticationBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
		HttpSecurity http = new HttpSecurity(objectPostProcessor, authenticationBuilder, new HashMap<>());
		StaticApplicationContext applicationContext = addApplicationContext(http);
		TestPropertyValues.of("spring.grpc.server.security.csrf.enabled=false").applyTo(applicationContext);
		addServiceDiscover(applicationContext);
		addGrpcServletRegistration(applicationContext);
		CsrfConfigurer<?> csrf = addCsrf(http);
		this.configurer.init(http);
		then(csrf).should(never()).requireCsrfProtectionMatcher(any());
	}

	@Test
	void initWhenEnabledPropertyTrueDisablesCsrf() {
		ObjectPostProcessor<Object> objectPostProcessor = ObjectPostProcessor.identity();
		AuthenticationManagerBuilder authenticationBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
		HttpSecurity http = new HttpSecurity(objectPostProcessor, authenticationBuilder, new HashMap<>());
		StaticApplicationContext applicationContext = addApplicationContext(http);
		TestPropertyValues.of("spring.grpc.server.security.csrf.enabled=true").applyTo(applicationContext);
		addServiceDiscover(applicationContext);
		addGrpcServletRegistration(applicationContext);
		CsrfConfigurer<?> csrf = addCsrf(http);
		this.configurer.init(http);
		ArgumentCaptor<RequestMatcher> matcher = ArgumentCaptor.captor();
		then(csrf).should().requireCsrfProtectionMatcher(matcher.capture());
		assertThat(matcher.getValue()).isSameAs(GrpcCsrfRequestMatcher.INSTANCE);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private CsrfConfigurer<?> addCsrf(HttpSecurity http) {
		CsrfConfigurer<?> csrf = mock();
		http.with((CsrfConfigurer) csrf);
		return csrf;
	}

	private StaticApplicationContext addApplicationContext(HttpSecurity http) {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		http.setSharedObject(ApplicationContext.class, applicationContext);
		return applicationContext;
	}

	private void addServiceDiscover(StaticApplicationContext applicationContext) {
		GrpcServiceDiscoverer serviceDiscoverer = mock();
		applicationContext.registerBean(GrpcServiceDiscoverer.class, serviceDiscoverer);
	}

	private void addGrpcServletRegistration(StaticApplicationContext applicationContext) {
		GrpcServletRegistration servletRegistration = mock();
		applicationContext.registerBean(GrpcServletRegistration.class, servletRegistration);
	}

}
