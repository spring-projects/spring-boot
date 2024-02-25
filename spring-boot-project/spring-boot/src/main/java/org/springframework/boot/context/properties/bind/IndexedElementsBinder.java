/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.context.properties.bind;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.bind.Binder.Context;
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

	/**
     * Constructs a new IndexedElementsBinder with the specified context.
     *
     * @param context the context to be used by the IndexedElementsBinder
     */
    IndexedElementsBinder(Context context) {
		super(context);
	}

	/**
     * Determines whether recursive binding is allowed for the given configuration property source.
     * 
     * @param source the configuration property source to check
     * @return {@code true} if recursive binding is allowed, {@code false} otherwise
     */
    @Override
	protected boolean isAllowRecursiveBinding(ConfigurationPropertySource source) {
		return source == null || source instanceof IterableConfigurationPropertySource;
	}

	/**
	 * Bind indexed elements to the supplied collection.
	 * @param name the name of the property to bind
	 * @param target the target bindable
	 * @param elementBinder the binder to use for elements
	 * @param aggregateType the aggregate type, may be a collection or an array
	 * @param elementType the element type
	 * @param result the destination for results
	 */
	protected final void bindIndexed(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder elementBinder, ResolvableType aggregateType, ResolvableType elementType,
			IndexedCollectionSupplier result) {
		for (ConfigurationPropertySource source : getContext().getSources()) {
			bindIndexed(source, name, target, elementBinder, result, aggregateType, elementType);
			if (result.wasSupplied() && result.get() != null) {
				return;
			}
		}
	}

	/**
     * Binds the indexed elements from the given configuration property source to the target bindable object.
     * 
     * @param source the configuration property source
     * @param root the root configuration property name
     * @param target the target bindable object
     * @param elementBinder the aggregate element binder
     * @param collection the indexed collection supplier
     * @param aggregateType the resolvable type of the aggregate
     * @param elementType the resolvable type of the elements
     */
    private void bindIndexed(ConfigurationPropertySource source, ConfigurationPropertyName root, Bindable<?> target,
			AggregateElementBinder elementBinder, IndexedCollectionSupplier collection, ResolvableType aggregateType,
			ResolvableType elementType) {
		ConfigurationProperty property = source.getConfigurationProperty(root);
		if (property != null) {
			getContext().setConfigurationProperty(property);
			bindValue(target, collection.get(), aggregateType, elementType, property.getValue());
		}
		else {
			bindIndexed(source, root, elementBinder, collection, elementType);
		}
	}

	/**
     * Binds a value to a target bindable collection.
     * 
     * @param target         the target bindable collection
     * @param collection     the collection to bind the value to
     * @param aggregateType  the ResolvableType of the aggregate object
     * @param elementType    the ResolvableType of the elements in the collection
     * @param value          the value to bind
     */
    private void bindValue(Bindable<?> target, Collection<Object> collection, ResolvableType aggregateType,
			ResolvableType elementType, Object value) {
		if (value == null || (value instanceof CharSequence charSequence && charSequence.isEmpty())) {
			return;
		}
		Object aggregate = convert(value, aggregateType, target.getAnnotations());
		ResolvableType collectionType = ResolvableType.forClassWithGenerics(collection.getClass(), elementType);
		Collection<Object> elements = convert(aggregate, collectionType);
		collection.addAll(elements);
	}

	/**
     * Binds indexed elements from the given configuration property source to the specified collection.
     * 
     * @param source the configuration property source
     * @param root the root configuration property name
     * @param elementBinder the aggregate element binder
     * @param collection the indexed collection supplier
     * @param elementType the resolvable type of the elements in the collection
     */
    private void bindIndexed(ConfigurationPropertySource source, ConfigurationPropertyName root,
			AggregateElementBinder elementBinder, IndexedCollectionSupplier collection, ResolvableType elementType) {
		MultiValueMap<String, ConfigurationPropertyName> knownIndexedChildren = getKnownIndexedChildren(source, root);
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			ConfigurationPropertyName name = root.append((i != 0) ? "[" + i + "]" : INDEX_ZERO);
			Object value = elementBinder.bind(name, Bindable.of(elementType), source);
			if (value == null) {
				break;
			}
			knownIndexedChildren.remove(name.getLastElement(Form.UNIFORM));
			collection.get().add(value);
		}
		assertNoUnboundChildren(source, knownIndexedChildren);
	}

	/**
     * Retrieves the known indexed children of a given root configuration property name from a configuration property source.
     * 
     * @param source The configuration property source to retrieve the children from.
     * @param root The root configuration property name.
     * @return A MultiValueMap containing the indexed children, where the key is the index and the value is the configuration property name.
     */
    private MultiValueMap<String, ConfigurationPropertyName> getKnownIndexedChildren(ConfigurationPropertySource source,
			ConfigurationPropertyName root) {
		MultiValueMap<String, ConfigurationPropertyName> children = new LinkedMultiValueMap<>();
		if (!(source instanceof IterableConfigurationPropertySource iterableSource)) {
			return children;
		}
		for (ConfigurationPropertyName name : iterableSource.filter(root::isAncestorOf)) {
			ConfigurationPropertyName choppedName = name.chop(root.getNumberOfElements() + 1);
			if (choppedName.isLastElementIndexed()) {
				String key = choppedName.getLastElement(Form.UNIFORM);
				children.add(key, name);
			}
		}
		return children;
	}

	/**
     * Asserts that there are no unbound children in the given configuration property source.
     * 
     * @param source the configuration property source
     * @param children the map of children configuration property names
     * @throws UnboundConfigurationPropertiesException if there are unbound children
     */
    private void assertNoUnboundChildren(ConfigurationPropertySource source,
			MultiValueMap<String, ConfigurationPropertyName> children) {
		if (!children.isEmpty()) {
			throw new UnboundConfigurationPropertiesException(children.values()
				.stream()
				.flatMap(List::stream)
				.map(source::getConfigurationProperty)
				.collect(Collectors.toCollection(TreeSet::new)));
		}
	}

	/**
     * Converts the given value to the specified type using the provided annotations.
     * 
     * @param value the value to be converted
     * @param type the ResolvableType representing the target type
     * @param annotations the annotations to be used during conversion
     * @return the converted value of type C
     */
    private <C> C convert(Object value, ResolvableType type, Annotation... annotations) {
		value = getContext().getPlaceholdersResolver().resolvePlaceholders(value);
		return getContext().getConverter().convert(value, type, annotations);
	}

	/**
	 * {@link AggregateBinder.AggregateSupplier AggregateSupplier} for an indexed
	 * collection.
	 */
	protected static class IndexedCollectionSupplier extends AggregateSupplier<Collection<Object>> {

		/**
         * Constructs a new IndexedCollectionSupplier with the specified supplier.
         * 
         * @param supplier the supplier used to create the collection
         */
        public IndexedCollectionSupplier(Supplier<Collection<Object>> supplier) {
			super(supplier);
		}

	}

}
