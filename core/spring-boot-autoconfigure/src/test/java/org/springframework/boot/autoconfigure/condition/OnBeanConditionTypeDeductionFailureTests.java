/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.condition.OnBeanCondition.BeanTypeDeductionException;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * Tests for {@link OnBeanCondition} when deduction of the bean's type fails
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("jackson-core-*.jar")
class OnBeanConditionTypeDeductionFailureTests {

	@Test
	void conditionalOnMissingBeanWithDeducedTypeThatIsPartiallyMissingFromClassPath() {
		assertThatException()
			.isThrownBy(() -> new AnnotationConfigApplicationContext(ImportingConfiguration.class).close())
			.satisfies((ex) -> {
				Throwable beanTypeDeductionException = findNestedCause(ex, BeanTypeDeductionException.class);
				assertThat(beanTypeDeductionException).isNotNull();
				assertThat(beanTypeDeductionException).hasMessage("Failed to deduce bean type for "
						+ OnMissingBeanConfiguration.class.getName() + ".objectMapper");
				assertThat(findNestedCause(beanTypeDeductionException, NoClassDefFoundError.class)).isNotNull();

			});
	}

	private @Nullable Throwable findNestedCause(Throwable ex, Class<? extends Throwable> target) {
		Throwable candidate = ex;
		while (candidate != null) {
			if (target.isInstance(candidate)) {
				return candidate;
			}
			candidate = candidate.getCause();
		}
		return null;
	}

	@Configuration(proxyBeanMethods = false)
	@Import(OnMissingBeanImportSelector.class)
	static class ImportingConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class OnMissingBeanConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

	}

	static class OnMissingBeanImportSelector implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[] { OnMissingBeanConfiguration.class.getName() };
		}

	}

}
