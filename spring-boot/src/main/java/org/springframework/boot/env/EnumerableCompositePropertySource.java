/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

/**
 * An mutable, enumerable, composite property source. New sources are added last (and
 * hence resolved with lowest priority).
 * 
 * @see PropertySource
 * @see EnumerablePropertySource
 * 
 * @author Dave Syer
 */
public class EnumerableCompositePropertySource extends
		EnumerablePropertySource<Collection<PropertySource<?>>> {

	private volatile String[] names;

	public EnumerableCompositePropertySource(String sourceName) {
		super(sourceName, new LinkedHashSet<PropertySource<?>>());
	}

	@Override
	public Object getProperty(String name) {
		for (PropertySource<?> propertySource : getSource()) {
			Object value = propertySource.getProperty(name);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	@Override
	public String[] getPropertyNames() {
		String[] result = this.names;
		if (result == null) {
			List<String> names = new ArrayList<String>();
			for (PropertySource<?> source : new ArrayList<PropertySource<?>>(getSource())) {
				if (source instanceof EnumerablePropertySource) {
					names.addAll(Arrays.asList(((EnumerablePropertySource<?>) source)
							.getPropertyNames()));
				}
			}
			this.names = names.toArray(new String[0]);
			result = this.names;
		}
		return result;
	}

	public void add(PropertySource<?> source) {
		getSource().add(source);
		this.names = null;
	}
}
