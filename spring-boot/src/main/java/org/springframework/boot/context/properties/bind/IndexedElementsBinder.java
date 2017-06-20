/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.context.properties.bind;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.bind.convert.BinderConversionService;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.IterableConfigurationPropertySource;
import org.springframework.core.ResolvableType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Base class for {@link AggregateBinder AggregateBinders} that read a sequential run of
 * indexed items.
 *
 * @param <T> the type being bound
 * @author Phillip Webb
 * @author Madhura Bhave
 */
abstract class IndexedElementsBinder<T> extends AggregateBinder<T> {

	private static final String INDEX_ZERO = "[0]";

	IndexedElementsBinder(BindContext context) {
		super(context);
	}

	protected final void bindIndexed(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder elementBinder, IndexedCollectionSupplier collection,
			ResolvableType aggregateType, ResolvableType elementType) {
		for (ConfigurationPropertySource source : getContext().getSources()) {
			bindIndexed(source, name, elementBinder, collection, aggregateType,
					elementType);
			if (collection.wasSupplied() && collection.get() != null) {
				return;
			}
		}
	}

	private void bindIndexed(ConfigurationPropertySource source,
			ConfigurationPropertyName root, AggregateElementBinder elementBinder,
			IndexedCollectionSupplier collection, ResolvableType aggregateType,
			ResolvableType elementType) {
		ConfigurationProperty property = source.getConfigurationProperty(root);
		if (property != null) {
			Object aggregate = convert(property.getValue(), aggregateType);
			ResolvableType collectionType = ResolvableType
					.forClassWithGenerics(collection.get().getClass(), elementType);
			Collection<Object> elements = convert(aggregate, collectionType);
			collection.get().addAll(elements);
		}
		else {
			bindIndexed(source, root, elementBinder, collection, elementType);
		}
	}

	private void bindIndexed(ConfigurationPropertySource source,
			ConfigurationPropertyName root, AggregateElementBinder elementBinder,
			IndexedCollectionSupplier collection, ResolvableType elementType) {
		MultiValueMap<String, ConfigurationProperty> knownIndexedChildren = getKnownIndexedChildren(
				source, root);
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			ConfigurationPropertyName name = root
					.append(i == 0 ? INDEX_ZERO : "[" + i + "]");
			Object value = elementBinder.bind(name, Bindable.of(elementType), source);
			if (value == null) {
				break;
			}
			knownIndexedChildren.remove(name.getLastElement(Form.UNIFORM));
			collection.get().add(value);
		}
		assertNoUnboundChildren(knownIndexedChildren);
	}

	private MultiValueMap<String, ConfigurationProperty> getKnownIndexedChildren(
			ConfigurationPropertySource source, ConfigurationPropertyName root) {
		MultiValueMap<String, ConfigurationProperty> children = new LinkedMultiValueMap<>();
		if (!(source instanceof IterableConfigurationPropertySource)) {
			return children;
		}
		for (ConfigurationPropertyName name : (IterableConfigurationPropertySource) source
				.filter(root::isAncestorOf)) {
			name = name.chop(root.getNumberOfElements() + 1);
			if (name.isLastElementIndexed()) {
				String key = name.getLastElement(Form.UNIFORM);
				ConfigurationProperty value = source.getConfigurationProperty(name);
				children.add(key, value);
			}
		}
		return children;
	}

	private void assertNoUnboundChildren(
			MultiValueMap<String, ConfigurationProperty> children) {
		if (!children.isEmpty()) {
			throw new UnboundConfigurationPropertiesException(
					children.values().stream().flatMap(List::stream)
							.collect(Collectors.toCollection(TreeSet::new)));
		}
	}

	@SuppressWarnings("unchecked")
	private <C> C convert(Object value, ResolvableType type) {
		value = getContext().getPlaceholdersResolver().resolvePlaceholders(value);
		BinderConversionService conversionService = getContext().getConversionService();
		return (C) conversionService.convert(value, type);
	}

	/**
	 * {@link AggregateBinder.AggregateSupplier AggregateSupplier} for an index
	 * collection.
	 */
	protected static class IndexedCollectionSupplier
			extends AggregateSupplier<Collection<Object>> {

		public IndexedCollectionSupplier(Supplier<Collection<Object>> supplier) {
			super(supplier);
		}

	}

}
