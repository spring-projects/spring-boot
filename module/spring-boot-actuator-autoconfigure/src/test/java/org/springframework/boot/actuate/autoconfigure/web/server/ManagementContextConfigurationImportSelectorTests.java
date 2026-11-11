/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.web.server;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManagementContextConfigurationImportSelector}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ManagementContextConfigurationImportSelectorTests {

	@Test
	void selectImportsShouldOrderResult() {
		String[] imports = new TestManagementContextConfigurationsImportSelector(C.class, A.class, D.class, B.class)
			.selectImports(AnnotationMetadata.introspect(EnableChildContext.class));
		assertThat(imports).containsExactly(A.class.getName(), B.class.getName(), C.class.getName(), D.class.getName());
	}

	@Test
	void selectImportsFiltersChildOnlyConfigurationWhenUsingSameContext() {
		String[] imports = new TestManagementContextConfigurationsImportSelector(ChildOnly.class, SameOnly.class,
				A.class)
			.selectImports(AnnotationMetadata.introspect(EnableSameContext.class));
		assertThat(imports).containsExactlyInAnyOrder(SameOnly.class.getName(), A.class.getName());
	}

	@Test
	void selectImportsFiltersSameOnlyConfigurationWhenUsingChildContext() {
		String[] imports = new TestManagementContextConfigurationsImportSelector(ChildOnly.class, SameOnly.class,
				A.class)
			.selectImports(AnnotationMetadata.introspect(EnableChildContext.class));
		assertThat(imports).containsExactlyInAnyOrder(ChildOnly.class.getName(), A.class.getName());
	}

	@Test
	void selectImportsLoadsFromResources() {
		String[] imports = new ManagementContextConfigurationImportSelector(
				ManagementContextConfigurationImportSelector.class.getClassLoader())
			.selectImports(AnnotationMetadata.introspect(EnableChildContext.class));
		Set<String> expected = new HashSet<>();
		ImportCandidates
			.load(ManagementContextConfiguration.class,
					ManagementContextConfigurationImportSelectorTests.class.getClassLoader())
			.forEach(expected::add);
		// Remove JerseySameManagementContextConfiguration, as it specifies
		// ManagementContextType.SAME and we asked for ManagementContextType.CHILD
		expected.remove(
				"org.springframework.boot.jersey.actuate.autoconfigure.web.JerseySameManagementContextConfiguration");
		assertThat(imports).containsExactlyInAnyOrderElementsOf(expected);
	}

	private static final class TestManagementContextConfigurationsImportSelector
			extends ManagementContextConfigurationImportSelector {

		private final List<String> factoryNames;

		private TestManagementContextConfigurationsImportSelector(Class<?>... classes) {
			super(ManagementContextConfigurationImportSelector.class.getClassLoader());
			this.factoryNames = Stream.of(classes).map(Class::getName).toList();
		}

		@Override
		protected List<String> loadFactoryNames() {
			return this.factoryNames;
		}

	}

	@Order(1)
	static class A {

	}

	@Order(2)
	static class B {

	}

	@Order(3)
	static class C {

	}

	static class D {

	}

	@ManagementContextConfiguration(ManagementContextType.CHILD)
	static class ChildOnly {

	}

	@ManagementContextConfiguration(ManagementContextType.SAME)
	static class SameOnly {

	}

	@EnableManagementContext(ManagementContextType.CHILD)
	static class EnableChildContext {

	}

	@EnableManagementContext(ManagementContextType.SAME)
	static class EnableSameContext {

	}

}
