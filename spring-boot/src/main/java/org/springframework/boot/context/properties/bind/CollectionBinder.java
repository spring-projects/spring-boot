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

	CollectionBinder(BindContext context) {
		super(context);
	}

	@Override
	protected Object bind(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder elementBinder, Class<?> type) {
		IndexedCollectionSupplier collection = new IndexedCollectionSupplier(
				() -> CollectionFactory.createCollection(type, 0));
		ResolvableType elementType = target.getType().asCollection().getGeneric();
		bindIndexed(name, target, elementBinder, collection, target.getType(),
				elementType);
		if (collection.wasSupplied()) {
			return collection.get();
		}
		return null;
	}

	@Override
	protected Collection<Object> merge(Collection<Object> existing,
			Collection<Object> additional) {
		try {
			existing.clear();
			existing.addAll(additional);
			return existing;
		}
		catch (UnsupportedOperationException ex) {
			return createNewCollection(additional);
		}
	}

	private Collection<Object> createNewCollection(Collection<Object> additional) {
		Collection<Object> merged = CollectionFactory
				.createCollection(additional.getClass(), additional.size());
		merged.addAll(additional);
		return merged;
	}

}
