/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.configurationprocessor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;
import org.springframework.boot.configurationprocessor.support.ConventionUtils;

/**
 * Resolve source configuration metadata for arbitrary types.
 *
 * @author Stephane Nicoll
 */
class ConfigurationPropertiesSourceResolver {

	private final ProcessingEnvironment processingEnvironment;

	private final TypeUtils typeUtils;

	ConfigurationPropertiesSourceResolver(ProcessingEnvironment processingEnvironment, TypeUtils typeUtils) {
		this.typeUtils = typeUtils;
		this.processingEnvironment = processingEnvironment;
	}

	/**
	 * Resolve the {@link SourceMetadata} for the specified type. If the type has no
	 * source metadata, return an {@link SourceMetadata#EMPTY} source.
	 * @param typeElement the type to discover source metadata from
	 * @return the source metadata for the specified type
	 */
	SourceMetadata resolveSource(TypeElement typeElement) {
		ConfigurationMetadata configurationMetadata = resolveConfigurationMetadata(typeElement);
		return (configurationMetadata != null)
				? new SourceMetadata(configurationMetadata.getItems(), configurationMetadata.getHints())
				: SourceMetadata.EMPTY;
	}

	private ConfigurationMetadata resolveConfigurationMetadata(TypeElement type) {
		try {
			String sourceLocation = MetadataStore.SOURCE_METADATA_PATH.apply(type, this.typeUtils);
			FileObject resource = this.processingEnvironment.getFiler()
				.getResource(StandardLocation.CLASS_PATH, "", sourceLocation);
			return (resource != null) ? new JsonMarshaller().read(resource.openInputStream()) : null;
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Additional source of metadata.
	 */
	static final class SourceMetadata {

		/**
		 * An empty source metadata.
		 */
		public static final SourceMetadata EMPTY = new SourceMetadata(Collections.emptyList(), Collections.emptyList());

		private final Map<String, ItemMetadata> items;

		private final Map<String, ItemHint> hints;

		private SourceMetadata(List<ItemMetadata> items, List<ItemHint> hints) {
			this.items = items.stream()
				.collect(Collectors.toMap((item) -> ConventionUtils.toDashedCase(item.getName()), Function.identity()));
			this.hints = hints.stream()
				.collect(Collectors.toMap((item) -> ConventionUtils.toDashedCase(item.getName()), Function.identity()));
		}

		/**
		 * Create a {@link PropertyDescriptor} for the given property name.
		 * @param name the name of a property
		 * @param propertyDescriptor the descriptor of the property
		 * @return a property descriptor that applies additional source metadata if
		 * necessary
		 */
		PropertyDescriptor createPropertyDescriptor(String name, PropertyDescriptor propertyDescriptor) {
			String key = ConventionUtils.toDashedCase(name);
			if (this.items.containsKey(key)) {
				ItemMetadata itemMetadata = this.items.get(key);
				ItemHint itemHint = this.hints.get(key);
				return new SourcePropertyDescriptor(propertyDescriptor, itemMetadata, itemHint);
			}
			return propertyDescriptor;
		}

		/**
		 * Create a {@link PropertyDescriptor} for the given property name.
		 * @param name the name of a property
		 * @param regularDescriptor a function to get the descriptor
		 * @return a property descriptor that applies additional source metadata if
		 * necessary
		 */
		PropertyDescriptor createPropertyDescriptor(String name,
				Function<String, PropertyDescriptor> regularDescriptor) {
			return createPropertyDescriptor(name, regularDescriptor.apply(name));
		}

	}

	/**
	 * A {@link PropertyDescriptor} that applies source metadata.
	 */
	static class SourcePropertyDescriptor extends PropertyDescriptor {

		private final PropertyDescriptor delegate;

		private final ItemMetadata sourceItemMetadata;

		private final ItemHint sourceItemHint;

		SourcePropertyDescriptor(PropertyDescriptor delegate, ItemMetadata sourceItemMetadata,
				ItemHint sourceItemHint) {
			super(delegate.getName(), delegate.getType(), delegate.getDeclaringElement(), delegate.getGetter());
			this.delegate = delegate;
			this.sourceItemMetadata = sourceItemMetadata;
			this.sourceItemHint = sourceItemHint;
		}

		@Override
		protected ItemHint resolveItemHint(String prefix, MetadataGenerationEnvironment environment) {
			return (this.sourceItemHint != null) ? this.sourceItemHint.applyPrefix(prefix)
					: super.resolveItemHint(prefix, environment);
		}

		@Override
		protected boolean isMarkedAsNested(MetadataGenerationEnvironment environment) {
			return this.delegate.isMarkedAsNested(environment);
		}

		@Override
		protected String resolveDescription(MetadataGenerationEnvironment environment) {
			String description = this.delegate.resolveDescription(environment);
			return (description != null) ? description : this.sourceItemMetadata.getDescription();
		}

		@Override
		protected Object resolveDefaultValue(MetadataGenerationEnvironment environment) {
			Object defaultValue = this.delegate.resolveDefaultValue(environment);
			return (defaultValue != null) ? defaultValue : this.sourceItemMetadata.getDefaultValue();
		}

		@Override
		protected ItemDeprecation resolveItemDeprecation(MetadataGenerationEnvironment environment) {
			ItemDeprecation itemDeprecation = this.delegate.resolveItemDeprecation(environment);
			return (itemDeprecation != null) ? itemDeprecation : this.sourceItemMetadata.getDeprecation();
		}

		@Override
		boolean isProperty(MetadataGenerationEnvironment environment) {
			return this.delegate.isProperty(environment);
		}

	}

}
