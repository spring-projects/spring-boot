/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.jmx;

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

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void objectNameMatchesManagedResourceByDefault() {
		this.contextRunner.withBean("testManagedResource", TestManagedResource.class).run((context) -> {
			ParentAwareNamingStrategy strategy = new ParentAwareNamingStrategy(new AnnotationJmxAttributeSource());
			strategy.setApplicationContext(context);
			assertThat(strategy.getObjectName(context.getBean("testManagedResource"), "testManagedResource")
				.getKeyPropertyListString()).isEqualTo("type=something,name1=def,name2=ghi");
		});
	}

	@Test
	void uniqueObjectNameAddsIdentityProperty() {
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
	void sameBeanInParentContextAddsContextProperty() {
		this.contextRunner.withBean("testManagedResource", TestManagedResource.class)
			.run((parent) -> this.contextRunner.withBean("testManagedResource", TestManagedResource.class)
				.withParent(parent)
				.run((context) -> {
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
	void uniqueObjectNameAndSameBeanInParentContextOnlyAddsIdentityProperty() {
		this.contextRunner.withBean("testManagedResource", TestManagedResource.class)
			.run((parent) -> this.contextRunner.withBean("testManagedResource", TestManagedResource.class)
				.withParent(parent)
				.run((context) -> {
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
