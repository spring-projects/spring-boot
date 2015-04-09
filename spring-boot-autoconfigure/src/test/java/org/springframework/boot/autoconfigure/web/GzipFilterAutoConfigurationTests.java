/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link GzipFilterAutoConfiguration}
 *
 * @author Andy Wilkinson
 */
public class GzipFilterAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void filterIsMappedToSlashStar() {
		createAndRefreshContext();
		FilterRegistrationBean registrationBean = this.context.getBean("gzipFilter",
				FilterRegistrationBean.class);
		assertThat(registrationBean.getUrlPatterns(), contains("/*"));
	}

	@Test
	public void byDefaultCheckGzExistsIsTheOnlyInitParameter() {
		createAndRefreshContext();
		FilterRegistrationBean registrationBean = this.context.getBean("gzipFilter",
				FilterRegistrationBean.class);
		assertThat(registrationBean.getInitParameters().size(), equalTo(1));
		assertThat(registrationBean.getInitParameters().get("checkGzExists"),
				equalTo("false"));
	}

	@Test
	public void customInitParameterConfiguration() {
		createAndRefreshContext("spring.http.gzip.bufferSize:1234",
				"spring.http.gzip.minGzipSize:2345",
				"spring.http.gzip.deflateCompressionLevel:5",
				"spring.http.gzip.deflateNoWrap:false",
				"spring.http.gzip.methods:GET,POST",
				"spring.http.gzip.mimeTypes:application/foo,application/bar",
				"spring.http.gzip.excludedAgents:excluded-agent-1,excluded-agent-2",
				"spring.http.gzip.excludedAgentPatterns:agent-pattern-1,agent-pattern-2",
				"spring.http.gzip.excludedPaths:/static/",
				"spring.http.gzip.excludedPathPatterns:path-pattern",
				"spring.http.gzip.vary:vary-header-value");
		FilterRegistrationBean registrationBean = this.context.getBean("gzipFilter",
				FilterRegistrationBean.class);
		assertThat(registrationBean.getInitParameters().size(), equalTo(12));
		assertThat(registrationBean.getInitParameters().get("checkGzExists"),
				equalTo("false"));
		assertThat(registrationBean.getInitParameters().get("bufferSize"),
				equalTo("1234"));
		assertThat(registrationBean.getInitParameters().get("minGzipSize"),
				equalTo("2345"));
		assertThat(registrationBean.getInitParameters().get("deflateCompressionLevel"),
				equalTo("5"));
		assertThat(registrationBean.getInitParameters().get("deflateNoWrap"),
				equalTo("false"));
		assertThat(registrationBean.getInitParameters().get("methods"),
				equalTo("GET,POST"));
		assertThat(registrationBean.getInitParameters().get("mimeTypes"),
				equalTo("application/foo,application/bar"));
		assertThat(registrationBean.getInitParameters().get("excludedAgents"),
				equalTo("excluded-agent-1,excluded-agent-2"));
		assertThat(registrationBean.getInitParameters().get("excludedAgentPatterns"),
				equalTo("agent-pattern-1,agent-pattern-2"));
		assertThat(registrationBean.getInitParameters().get("excludedPaths"),
				equalTo("/static/"));
		assertThat(registrationBean.getInitParameters().get("excludedPathPatterns"),
				equalTo("path-pattern"));
		assertThat(registrationBean.getInitParameters().get("vary"),
				equalTo("vary-header-value"));
	}

	@Test
	public void filterCanBeDisabled() {
		createAndRefreshContext("spring.http.gzip.enabled:false");
		assertThat(this.context.getBeanNamesForType(FilterRegistrationBean.class).length,
				is(equalTo(0)));
	}

	private void createAndRefreshContext(String... pairs) {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, pairs);
		this.context.register(GzipFilterAutoConfiguration.class);
		this.context.refresh();
	}

}
