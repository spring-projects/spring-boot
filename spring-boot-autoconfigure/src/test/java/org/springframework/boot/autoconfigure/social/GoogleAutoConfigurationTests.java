/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.social;

import org.junit.Test;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.social.google.api.Google;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link GoogleAutoConfiguration}.
 *
 * @author Yuan Ji
 */
public class GoogleAutoConfigurationTests extends AbstractSocialAutoConfigurationTests {

	@Test
	public void expectedSocialBeansCreated() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.social.google.appId:12345");
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.social.google.appSecret:secret");
		this.context.register(GoogleAutoConfiguration.class);
		this.context.register(SocialWebAutoConfiguration.class);
		this.context.refresh();
		assertConnectionFrameworkBeans();
		assertNotNull(this.context.getBean(Google.class));
	}

	@Test
	public void noGoogleBeanCreatedWhenPropertiesArentSet() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(GoogleAutoConfiguration.class);
		this.context.register(SocialWebAutoConfiguration.class);
		this.context.refresh();
		assertNoConnectionFrameworkBeans();
		assertMissingBean(Google.class);
	}
}
