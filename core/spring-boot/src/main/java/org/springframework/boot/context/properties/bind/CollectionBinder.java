/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.bind;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.bind.Binder.Context;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.CollectionFactory;
import org.springframework.core.ResolvableType;

/**
 * {@link AggregateBinder} for collections.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class CollectionBinder extends IndexedElementsBinder<Collection<Object>> {

	CollectionBinder(Context context) {
		super(context);
	}

	@Override
	protected @Nullable Object bindAggregate(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder elementBinder) {
		ResolvableType aggregateType = ResolvableType.forClassWithGenerics(List.class,
				target.getType().asCollection().getGenerics());
		ResolvableType elementType = target.getType().asCollection().getGeneric();
		IndexedCollectionSupplier result = new IndexedCollectionSupplier(
				() -> CollectionFactory.createCollection(List.class, elementType.resolve(), 0));
		bindIndexed(name, target, elementBinder, aggregateType, elementType, result);
		if (result.wasSupplied()) {
			return result.get();
		}
		return null;
	}

	@Override
	protected Collection<Object> merge(Supplier<Collection<Object>> existing, Collection<Object> additional) {
		Collection<Object> existingCollection = getExistingIfPossible(existing);
		if (existingCollection == null) {
			return additional;
		}
		try {
			existingCollection.clear();
			existingCollection.addAll(additional);
			return copyIfPossible(existingCollection);
		}
		catch (UnsupportedOperationException ex) {
			return createNewCollection(additional);
		}
	}

	private @Nullable Collection<Object> getExistingIfPossible(Supplier<Collection<Object>> existing) {
		try {
			return existing.get();
		}
		catch (Exception ex) {
			return null;
		}
	}

	private Collection<Object> copyIfPossible(Collection<Object> collection) {
		try {
			return createNewCollection(collection);
		}
		catch (Exception ex) {
			return collection;
		}
	}

	private Collection<Object> createNewCollection(Collection<Object> collection) {
		Collection<Object> result = CollectionFactory.createCollection(collection.getClass(), collection.size());
		result.addAll(collection);
		return result;
	}

}
