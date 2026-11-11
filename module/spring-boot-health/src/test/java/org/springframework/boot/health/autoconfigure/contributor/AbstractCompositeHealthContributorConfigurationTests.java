/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.contributor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AbstractCompositeHealthContributorConfiguration}.
 *
 * @param <C> the contributor type
 * @param <I> the health indicator type
 * @author Phillip Webb
 */
abstract class AbstractCompositeHealthContributorConfigurationTests<C, I extends C> {

	private final @Nullable Class<?> indicatorType;

	AbstractCompositeHealthContributorConfigurationTests() {
		ResolvableType type = ResolvableType.forClass(AbstractCompositeHealthContributorConfigurationTests.class,
				getClass());
		this.indicatorType = type.resolveGeneric(1);
	}

	@Test
	void createContributorWhenBeansIsEmptyThrowsException() {
		Map<String, TestBean> beans = Collections.emptyMap();
		assertThatIllegalArgumentException().isThrownBy(() -> newComposite().createContributor(beans))
			.withMessage("'beans' must not be empty");
	}

	@Test
	void createContributorWhenBeansHasSingleElementCreatesIndicator() {
		Map<String, TestBean> beans = Collections.singletonMap("test", new TestBean());
		C contributor = newComposite().createContributor(beans);
		assertThat(contributor).isInstanceOf(this.indicatorType);
	}

	@Test
	void createContributorWhenBeansHasMultipleElementsCreatesComposite() {
		Map<String, TestBean> beans = new LinkedHashMap<>();
		beans.put("test1", new TestBean());
		beans.put("test2", new TestBean());
		C contributor = newComposite().createContributor(beans);
		assertThat(contributor).isNotInstanceOf(this.indicatorType);
		assertThat(ClassUtils.getShortName(contributor.getClass())).startsWith("MapComposite");
	}

	@Test
	void createContributorWhenBeanFactoryHasNoBeansThrowsException() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.refresh();
			assertThatIllegalArgumentException()
				.isThrownBy(() -> newComposite().createContributor(context.getBeanFactory(), TestBean.class));
		}
	}

	@Test
	void createContributorWhenBeanFactoryHasSingleBeanCreatesIndicator() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.register(SingleBeanConfiguration.class);
			context.refresh();
			C contributor = newComposite().createContributor(context.getBeanFactory(), TestBean.class);
			assertThat(contributor).isInstanceOf(this.indicatorType);
		}
	}

	@Test
	void createContributorWhenBeanFactoryHasMultipleBeansCreatesComposite() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.register(MultipleBeansConfiguration.class);
			context.refresh();
			C contributor = newComposite().createContributor(context.getBeanFactory(), TestBean.class);
			assertThat(contributor).isNotInstanceOf(this.indicatorType);
			assertThat(ClassUtils.getShortName(contributor.getClass())).startsWith("MapComposite");
			assertThat(allNamesFromComposite(contributor)).containsExactlyInAnyOrder("standard", "nonDefault");
		}
	}

	protected abstract AbstractCompositeHealthContributorConfiguration<C, I, TestBean> newComposite();

	protected abstract Stream<String> allNamesFromComposite(C composite);

	static class TestBean {

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleBeansConfiguration {

		@Bean
		TestBean standard() {
			return new TestBean();
		}

		@Bean(defaultCandidate = false)
		TestBean nonDefault() {
			return new TestBean();
		}

		@Bean(autowireCandidate = false)
		TestBean nonAutowire() {
			return new TestBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SingleBeanConfiguration {

		@Bean
		TestBean standard() {
			return new TestBean();
		}

		@Bean(autowireCandidate = false)
		TestBean nonAutowire() {
			return new TestBean();
		}

	}

}
