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
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.linkedin.api.LinkedIn;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Spring Social configuration with multiple API providers.
 *
 * @author Craig Walls
 */
public class MultiApiAutoConfigurationTests extends AbstractSocialAutoConfigurationTests {

	@Test
	public void expectTwitterConfigurationOnly() throws Exception {
		setupContext("spring.social.twitter.appId:12345",
				"spring.social.twitter.appSecret:secret");
		assertConnectionFrameworkBeans();
		assertThat(this.context.getBean(Twitter.class)).isNotNull();
		assertMissingBean(Facebook.class);
		assertMissingBean(LinkedIn.class);
	}

	@Test
	public void expectFacebookConfigurationOnly() throws Exception {
		setupContext("spring.social.facebook.appId:12345",
				"spring.social.facebook.appSecret:secret");
		assertConnectionFrameworkBeans();
		assertThat(this.context.getBean(Facebook.class)).isNotNull();
		assertMissingBean(Twitter.class);
		assertMissingBean(LinkedIn.class);
	}

	@Test
	public void expectLinkedInConfigurationOnly() throws Exception {
		setupContext("spring.social.linkedin.appId:12345",
				"spring.social.linkedin.appSecret:secret");
		assertConnectionFrameworkBeans();
		assertThat(this.context.getBean(LinkedIn.class)).isNotNull();
		assertMissingBean(Twitter.class);
		assertMissingBean(Facebook.class);
	}

	@Test
	public void expectFacebookAndLinkedInConfigurationOnly() throws Exception {
		setupContext("spring.social.facebook.appId:54321",
				"spring.social.facebook.appSecret:shhhhh",
				"spring.social.linkedin.appId:12345",
				"spring.social.linkedin.appSecret:secret");
		assertConnectionFrameworkBeans();
		assertThat(this.context.getBean(Facebook.class)).isNotNull();
		assertThat(this.context.getBean(LinkedIn.class)).isNotNull();
		assertMissingBean(Twitter.class);
	}

	@Test
	public void expectFacebookAndTwitterConfigurationOnly() throws Exception {
		setupContext("spring.social.facebook.appId:54321",
				"spring.social.facebook.appSecret:shhhhh",
				"spring.social.twitter.appId:12345",
				"spring.social.twitter.appSecret:secret");
		assertConnectionFrameworkBeans();
		assertThat(this.context.getBean(Facebook.class)).isNotNull();
		assertThat(this.context.getBean(Twitter.class)).isNotNull();
		assertMissingBean(LinkedIn.class);
	}

	@Test
	public void expectLinkedInAndTwitterConfigurationOnly() throws Exception {
		setupContext("spring.social.linkedin.appId:54321",
				"spring.social.linkedin.appSecret:shhhhh",
				"spring.social.twitter.appId:12345",
				"spring.social.twitter.appSecret:secret");
		assertConnectionFrameworkBeans();
		assertThat(this.context.getBean(LinkedIn.class)).isNotNull();
		assertThat(this.context.getBean(Twitter.class)).isNotNull();
		assertMissingBean(Facebook.class);
	}

	@Test
	public void noSocialBeansCreatedWhenPropertiesArentSet() throws Exception {
		setupContext();
		assertNoConnectionFrameworkBeans();
		assertMissingBean(Twitter.class);
		assertMissingBean(Facebook.class);
		assertMissingBean(LinkedIn.class);
	}

	private void setupContext(String... environment) {
		this.context = new AnnotationConfigWebApplicationContext();
		TestPropertyValues.of(environment).applyTo(this.context);
		ConfigurationPropertySources.attach(this.context.getEnvironment());
		this.context.register(TwitterAutoConfiguration.class);
		this.context.register(FacebookAutoConfiguration.class);
		this.context.register(LinkedInAutoConfiguration.class);
		this.context.register(SocialWebAutoConfiguration.class);
		this.context.refresh();
	}

}
