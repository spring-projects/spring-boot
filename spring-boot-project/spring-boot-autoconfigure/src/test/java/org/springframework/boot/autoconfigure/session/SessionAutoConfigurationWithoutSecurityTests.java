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

package org.springframework.boot.autoconfigure.session;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.session.web.http.DefaultCookieSerializer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SessionAutoConfiguration} when Spring Security is not on the
 * classpath.
 *
 * @author Vedran Pavic
 */
@ClassPathExclusions("spring-security-*")
class SessionAutoConfigurationWithoutSecurityTests extends AbstractSessionAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SessionAutoConfiguration.class));

	@Test
	void sessionCookieConfigurationIsAppliedToAutoConfiguredCookieSerializer() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class).run((context) -> {
			DefaultCookieSerializer cookieSerializer = context.getBean(DefaultCookieSerializer.class);
			assertThat(cookieSerializer).hasFieldOrPropertyWithValue("rememberMeRequestAttribute", null);
		});
	}

}
