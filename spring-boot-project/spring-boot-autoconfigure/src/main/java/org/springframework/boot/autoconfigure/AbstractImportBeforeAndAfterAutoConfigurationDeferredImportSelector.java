/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link DeferredImportSelector} implementation that works with
 * {@link ImportBeforeAutoConfiguration} and {@link ImportAfterAutoConfiguration}.
 *
 * @author Tadaya Tsuyukubo
 * @since 2.3.0
 */
public abstract class AbstractImportBeforeAndAfterAutoConfigurationDeferredImportSelector
		implements DeferredImportSelector, Ordered {

	// ImportAutoConfigurationImportSelector is a child of AutoConfigurationImportSelector
	// and getOrder() subtracts 1 from parent; therefore, use it as a lower bound.
	protected static final int ORDER_BEFORE_AUTO_CONFIGURATION = new ImportAutoConfigurationImportSelector().getOrder()
			- 1;

	protected static final int ORDER_AFTER_AUTO_CONFIGURATION = new AutoConfigurationImportSelector().getOrder() + 1;

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		Class<?> annotationClass = getAnnotationClass();
		AnnotationAttributes attributes = getAttributes(metadata, annotationClass, true);
		String[] imports = attributes.getStringArray("classes");
		Assert.notNull(imports,
				() -> "Attribute \"classes\" should exist on " + ClassUtils.getShortName(annotationClass));
		return imports;
	}

	/**
	 * Return the source annotation class used by the selector.
	 * @return the annotation class
	 */
	protected abstract Class<?> getAnnotationClass();

	private static AnnotationAttributes getAttributes(AnnotationMetadata metadata, Class<?> annotationClass,
			boolean classValuesAsString) {
		String annotationName = annotationClass.getName();
		AnnotationAttributes attributes = AnnotationAttributes
				.fromMap(metadata.getAnnotationAttributes(annotationName, classValuesAsString));
		Assert.notNull(attributes, () -> "No annotation attributes found. Is " + metadata.getClassName()
				+ " annotated with " + ClassUtils.getShortName(annotationName) + "?");
		return attributes;
	}

	/**
	 * {@link DeferredImportSelector.Group} implementation for
	 * {@link ImportBeforeAutoConfigurationDeferredImportSelector} and
	 * {@link ImportAfterAutoConfigurationDeferredImportSelector}.
	 */
	protected abstract static class AbstractBeforeAndAfterAutoConfigurationGroup
			implements DeferredImportSelector.Group {

		private static final Comparator<ConfigurationEntry> CONFIGURATION_CLASS_COMPARATOR = (o1,
				o2) -> AnnotationAwareOrderComparator.INSTANCE.compare(o1.getImportingConfigClass(),
						o2.getImportingConfigClass());

		private List<ConfigurationEntry> entries = new ArrayList<>();

		@Override
		public void process(AnnotationMetadata metadata, DeferredImportSelector selector) {
			Assert.state(selector instanceof AbstractImportBeforeAndAfterAutoConfigurationDeferredImportSelector,
					() -> String.format("Only %s implementations are supported, got %s",
							AbstractImportBeforeAndAfterAutoConfigurationDeferredImportSelector.class.getSimpleName(),
							selector.getClass().getName()));
			Class<?> annotationClass = ((AbstractImportBeforeAndAfterAutoConfigurationDeferredImportSelector) selector)
					.getAnnotationClass();
			AnnotationAttributes attributes = getAttributes(metadata, annotationClass, false);
			Class<?>[] configurationClasses = attributes.getClassArray("classes");
			for (Class<?> configurationClass : configurationClasses) {
				ConfigurationEntry entry = new ConfigurationEntry(configurationClass, metadata);
				this.entries.add(entry);
			}
			this.entries.sort(CONFIGURATION_CLASS_COMPARATOR);
		}

		@Override
		public Iterable<Entry> selectImports() {
			return new ArrayList<>(this.entries);
		}

		private static class ConfigurationEntry extends Entry {

			private final Class<?> importingConfigClass;

			ConfigurationEntry(Class<?> importingConfigClass, AnnotationMetadata metadata) {
				super(metadata, importingConfigClass.getName());
				this.importingConfigClass = importingConfigClass;
			}

			Class<?> getImportingConfigClass() {
				return this.importingConfigClass;
			}

		}

	}

}
