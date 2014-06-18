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

import static org.junit.Assert.*;

import org.junit.After;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.social.UserIdSource;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Abstract base class for testing Spring Social autoconfiguration.
 *
 * @author Craig Walls
 * @Since 1.1.2
 */
public class AbstractSocialAutoConfigurationTests {

	protected AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	public AbstractSocialAutoConfigurationTests() {
		super();
	}

	protected void assertConnectionFrameworkBeans() {
		assertNotNull(this.context.getBean(UsersConnectionRepository.class));
		assertNotNull(this.context.getBean(ConnectionRepository.class));
		assertNotNull(this.context.getBean(ConnectionFactoryLocator.class));
		assertNotNull(this.context.getBean(UserIdSource.class));
	}

	protected void assertNoConnectionFrameworkBeans() {
		assertMissingBean(UsersConnectionRepository.class);
		assertMissingBean(ConnectionRepository.class);
		assertMissingBean(ConnectionFactoryLocator.class);
		assertMissingBean(UserIdSource.class);
	}

	protected void assertMissingBean(Class<?> clazz) {
		try {
			assertNotNull(this.context.getBean(clazz));
			fail("Unexpected bean in context of type " + clazz.getName());
		} catch (NoSuchBeanDefinitionException e) {
		}
	}

}