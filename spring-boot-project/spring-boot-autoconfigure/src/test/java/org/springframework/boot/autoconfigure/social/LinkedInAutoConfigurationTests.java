/*
 * Copyright 2012-2017 the original author or authors.
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

import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.social.linkedin.api.LinkedIn;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LinkedInAutoConfiguration}.
 *
 * @author Craig Walls
 */
public class LinkedInAutoConfigurationTests extends AbstractSocialAutoConfigurationTests {

	@Test
	public void expectedSocialBeansCreated() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		TestPropertyValues.of("spring.social.linkedin.appId:12345").applyTo(this.context);
		TestPropertyValues.of("spring.social.linkedin.appSecret:secret")
				.applyTo(this.context);
		ConfigurationPropertySources.attach(this.context.getEnvironment());
		this.context.register(LinkedInAutoConfiguration.class);
		this.context.register(SocialWebAutoConfiguration.class);
		this.context.refresh();
		assertConnectionFrameworkBeans();
		assertThat(this.context.getBean(LinkedIn.class)).isNotNull();
	}

	@Test
	public void noLinkedInBeanCreatedIfPropertiesArentSet() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(LinkedInAutoConfiguration.class);
		this.context.register(SocialWebAutoConfiguration.class);
		this.context.refresh();
		assertNoConnectionFrameworkBeans();
		assertMissingBean(LinkedIn.class);
	}

}
