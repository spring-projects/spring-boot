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

package org.springframework.boot.autoconfigure.domain;

import java.util.Collections;
import java.util.Set;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.autoconfigure.domain.scan.a.EmbeddableA;
import org.springframework.boot.autoconfigure.domain.scan.a.EntityA;
import org.springframework.boot.autoconfigure.domain.scan.b.EmbeddableB;
import org.springframework.boot.autoconfigure.domain.scan.b.EntityB;
import org.springframework.boot.autoconfigure.domain.scan.c.EmbeddableC;
import org.springframework.boot.autoconfigure.domain.scan.c.EntityC;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EntityScanner}.
 *
 * @author Phillip Webb
 */
class EntityScannerTests {

	@Test
	void createWhenContextIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new EntityScanner(null))
				.withMessageContaining("Context must not be null");
	}

	@Test
	void scanShouldScanFromSinglePackage() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ScanConfig.class);
		EntityScanner scanner = new EntityScanner(context);
		Set<Class<?>> scanned = scanner.scan(Entity.class);
		assertThat(scanned).containsOnly(EntityA.class, EntityB.class, EntityC.class);
		context.close();
	}

	@Test
	void scanShouldScanFromResolvedPlaceholderPackage() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("com.example.entity-package=org.springframework.boot.autoconfigure.domain.scan")
				.applyTo(context);
		context.register(ScanPlaceholderConfig.class);
		context.refresh();
		EntityScanner scanner = new EntityScanner(context);
		Set<Class<?>> scanned = scanner.scan(Entity.class);
		assertThat(scanned).containsOnly(EntityA.class, EntityB.class, EntityC.class);
		context.close();
	}

	@Test
	void scanShouldScanFromMultiplePackages() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ScanAConfig.class,
				ScanBConfig.class);
		EntityScanner scanner = new EntityScanner(context);
		Set<Class<?>> scanned = scanner.scan(Entity.class);
		assertThat(scanned).containsOnly(EntityA.class, EntityB.class);
		context.close();
	}

	@Test
	void scanShouldFilterOnAnnotation() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ScanConfig.class);
		EntityScanner scanner = new EntityScanner(context);
		assertThat(scanner.scan(Entity.class)).containsOnly(EntityA.class, EntityB.class, EntityC.class);
		assertThat(scanner.scan(Embeddable.class)).containsOnly(EmbeddableA.class, EmbeddableB.class,
				EmbeddableC.class);
		assertThat(scanner.scan(Entity.class, Embeddable.class)).containsOnly(EntityA.class, EntityB.class,
				EntityC.class, EmbeddableA.class, EmbeddableB.class, EmbeddableC.class);
		context.close();
	}

	@Test
	void scanShouldUseCustomCandidateComponentProvider() throws ClassNotFoundException {
		ClassPathScanningCandidateComponentProvider candidateComponentProvider = mock(
				ClassPathScanningCandidateComponentProvider.class);
		given(candidateComponentProvider.findCandidateComponents("org.springframework.boot.autoconfigure.domain.scan"))
				.willReturn(Collections.emptySet());
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ScanConfig.class);
		TestEntityScanner scanner = new TestEntityScanner(context, candidateComponentProvider);
		scanner.scan(Entity.class);
		ArgumentCaptor<AnnotationTypeFilter> annotationTypeFilter = ArgumentCaptor.forClass(AnnotationTypeFilter.class);
		then(candidateComponentProvider).should().addIncludeFilter(annotationTypeFilter.capture());
		then(candidateComponentProvider).should()
				.findCandidateComponents("org.springframework.boot.autoconfigure.domain.scan");
		then(candidateComponentProvider).shouldHaveNoMoreInteractions();
		assertThat(annotationTypeFilter.getValue().getAnnotationType()).isEqualTo(Entity.class);
	}

	@Test
	void scanShouldScanCommaSeparatedPackagesInPlaceholderPackage() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(
				"com.example.entity-package=org.springframework.boot.autoconfigure.domain.scan.a,org.springframework.boot.autoconfigure.domain.scan.b")
				.applyTo(context);
		context.register(ScanPlaceholderConfig.class);
		context.refresh();
		EntityScanner scanner = new EntityScanner(context);
		Set<Class<?>> scanned = scanner.scan(Entity.class);
		assertThat(scanned).containsOnly(EntityA.class, EntityB.class);
		context.close();
	}

	private static class TestEntityScanner extends EntityScanner {

		private final ClassPathScanningCandidateComponentProvider candidateComponentProvider;

		TestEntityScanner(ApplicationContext context,
				ClassPathScanningCandidateComponentProvider candidateComponentProvider) {
			super(context);
			this.candidateComponentProvider = candidateComponentProvider;
		}

		@Override
		protected ClassPathScanningCandidateComponentProvider createClassPathScanningCandidateComponentProvider(
				ApplicationContext context) {
			return this.candidateComponentProvider;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan("org.springframework.boot.autoconfigure.domain.scan")
	static class ScanConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan(basePackageClasses = EntityA.class)
	static class ScanAConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan(basePackageClasses = EntityB.class)
	static class ScanBConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan("${com.example.entity-package}")
	static class ScanPlaceholderConfig {

	}

}
