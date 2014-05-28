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

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.social.UserIdSource;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.linkedin.api.LinkedIn;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link LinkedInAutoConfiguration}.
 *
 * @author Craig Walls
 */
public class LinkedInAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void expectedSocialBeansCreated() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.social.linkedin.appId:12345");
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.social.linkedin.appSecret:secret");
		this.context.register(SocialWebAutoConfiguration.class);
		this.context.register(LinkedInAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(UsersConnectionRepository.class));
		assertNotNull(this.context.getBean(ConnectionRepository.class));
		assertNotNull(this.context.getBean(ConnectionFactoryLocator.class));
		assertNotNull(this.context.getBean(UserIdSource.class));
		assertNotNull(this.context.getBean(LinkedIn.class));
	}

}
