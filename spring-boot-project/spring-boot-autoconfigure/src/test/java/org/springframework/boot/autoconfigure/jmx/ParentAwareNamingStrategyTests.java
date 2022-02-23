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

package org.springframework.boot.autoconfigure.jmx;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ParentAwareNamingStrategy}.
 *
 * @author Andy Wilkinson
 */
class ParentAwareNamingStrategyTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void objectNameMatchesManagedResourceByDefault() throws MalformedObjectNameException {
		this.contextRunner.withBean("testManagedResource", TestManagedResource.class).run((context) -> {
			ParentAwareNamingStrategy strategy = new ParentAwareNamingStrategy(new AnnotationJmxAttributeSource());
			strategy.setApplicationContext(context);
			assertThat(strategy.getObjectName(context.getBean("testManagedResource"), "testManagedResource")
					.getKeyPropertyListString()).isEqualTo("type=something,name1=def,name2=ghi");
		});
	}

	@Test
	void uniqueObjectNameAddsIdentityProperty() throws MalformedObjectNameException {
		this.contextRunner.withBean("testManagedResource", TestManagedResource.class).run((context) -> {
			ParentAwareNamingStrategy strategy = new ParentAwareNamingStrategy(new AnnotationJmxAttributeSource());
			strategy.setApplicationContext(context);
			strategy.setEnsureUniqueRuntimeObjectNames(true);
			Object resource = context.getBean("testManagedResource");
			ObjectName objectName = strategy.getObjectName(resource, "testManagedResource");
			assertThat(objectName.getDomain()).isEqualTo("ABC");
			assertThat(objectName.getCanonicalKeyPropertyListString()).isEqualTo(
					"identity=" + ObjectUtils.getIdentityHexString(resource) + ",name1=def,name2=ghi,type=something");
		});
	}

	@Test
	void sameBeanInParentContextAddsContextProperty() throws MalformedObjectNameException {
		this.contextRunner.withBean("testManagedResource", TestManagedResource.class).run((parent) -> this.contextRunner
				.withBean("testManagedResource", TestManagedResource.class).withParent(parent).run((context) -> {
					ParentAwareNamingStrategy strategy = new ParentAwareNamingStrategy(
							new AnnotationJmxAttributeSource());
					strategy.setApplicationContext(context);
					Object resource = context.getBean("testManagedResource");
					ObjectName objectName = strategy.getObjectName(resource, "testManagedResource");
					assertThat(objectName.getDomain()).isEqualTo("ABC");
					assertThat(objectName.getCanonicalKeyPropertyListString()).isEqualTo("context="
							+ ObjectUtils.getIdentityHexString(context) + ",name1=def,name2=ghi,type=something");
				}));
	}

	@Test
	void uniqueObjectNameAndSameBeanInParentContextOnlyAddsIdentityProperty() throws MalformedObjectNameException {
		this.contextRunner.withBean("testManagedResource", TestManagedResource.class).run((parent) -> this.contextRunner
				.withBean("testManagedResource", TestManagedResource.class).withParent(parent).run((context) -> {
					ParentAwareNamingStrategy strategy = new ParentAwareNamingStrategy(
							new AnnotationJmxAttributeSource());
					strategy.setApplicationContext(context);
					strategy.setEnsureUniqueRuntimeObjectNames(true);
					Object resource = context.getBean("testManagedResource");
					ObjectName objectName = strategy.getObjectName(resource, "testManagedResource");
					assertThat(objectName.getDomain()).isEqualTo("ABC");
					assertThat(objectName.getCanonicalKeyPropertyListString()).isEqualTo("identity="
							+ ObjectUtils.getIdentityHexString(resource) + ",name1=def,name2=ghi,type=something");
				}));
	}

	@ManagedResource(objectName = "ABC:type=something,name1=def,name2=ghi")
	public static class TestManagedResource {

	}

}
