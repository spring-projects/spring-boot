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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.ResolvableType;

/**
 * {@link AggregateBinder} for arrays.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ArrayBinder extends IndexedElementsBinder<Object> {

	ArrayBinder(BindContext context) {
		super(context);
	}

	@Override
	protected Object bind(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder elementBinder, Class<?> type) {
		IndexedCollectionSupplier collection = new IndexedCollectionSupplier(
				ArrayList::new);
		ResolvableType elementType = target.getType().getComponentType();
		bindIndexed(name, target, elementBinder, collection, target.getType(),
				elementType);
		if (collection.wasSupplied()) {
			List<Object> list = (List<Object>) collection.get();
			Object array = Array.newInstance(elementType.resolve(), list.size());
			for (int i = 0; i < list.size(); i++) {
				Array.set(array, i, list.get(i));
			}
			return array;
		}
		return null;
	}

	@Override
	protected Object merge(Object existing, Object additional) {
		return additional;
	}

}
