/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.context.bootstrap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpringBootTestContextBootstrapper}.
 *
 * @author Andy Wilkinson
 */
public class SpringBootTestContextBootstrapperTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@SuppressWarnings("rawtypes")
	@Test
	public void failFastWhenSpringBootTestAndWebAppConfigurationAreUsedTogether() {
		SpringBootTestContextBootstrapper bootstrapper = new SpringBootTestContextBootstrapper();
		BootstrapContext bootstrapContext = mock(BootstrapContext.class);
		bootstrapper.setBootstrapContext(bootstrapContext);
		given((Class) bootstrapContext.getTestClass())
				.willReturn(SpringBootTestAndWebAppConfiguration.class);
		CacheAwareContextLoaderDelegate contextLoaderDeleagte = mock(
				CacheAwareContextLoaderDelegate.class);
		given(bootstrapContext.getCacheAwareContextLoaderDelegate())
				.willReturn(contextLoaderDeleagte);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("@WebAppConfiguration is unnecessary when using "
				+ "@SpringBootTest and should be removed");
		bootstrapper.buildTestContext();
	}

	@SpringBootTest
	@WebAppConfiguration
	private static class SpringBootTestAndWebAppConfiguration {

	}

}
