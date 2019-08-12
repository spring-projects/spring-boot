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

package org.springframework.boot.configurationmetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Hints of an item to provide the list of values and/or the name of the provider
 * responsible to identify suitable values. If the type of the related item is a
 * {@link java.util.Map} it can have both key and value hints.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class Hints {

	private final List<ValueHint> keyHints = new ArrayList<>();

	private final List<ValueProvider> keyProviders = new ArrayList<>();

	private final List<ValueHint> valueHints = new ArrayList<>();

	private final List<ValueProvider> valueProviders = new ArrayList<>();

	/**
	 * The list of well-defined keys, if any. Only applicable if the type of the related
	 * item is a {@link java.util.Map}. If no extra {@link ValueProvider provider} is
	 * specified, these values are to be considered a closed-set of the available keys for
	 * the map.
	 * @return the key hints
	 */
	public List<ValueHint> getKeyHints() {
		return this.keyHints;
	}

	/**
	 * The value providers that are applicable to the keys of this item. Only applicable
	 * if the type of the related item is a {@link java.util.Map}. Only one
	 * {@link ValueProvider} is enabled for a key: the first in the list that is supported
	 * should be used.
	 * @return the key providers
	 */
	public List<ValueProvider> getKeyProviders() {
		return this.keyProviders;
	}

	/**
	 * The list of well-defined values, if any. If no extra {@link ValueProvider provider}
	 * is specified, these values are to be considered a closed-set of the available
	 * values for this item.
	 * @return the value hints
	 */
	public List<ValueHint> getValueHints() {
		return this.valueHints;
	}

	/**
	 * The value providers that are applicable to this item. Only one
	 * {@link ValueProvider} is enabled for an item: the first in the list that is
	 * supported should be used.
	 * @return the value providers
	 */
	public List<ValueProvider> getValueProviders() {
		return this.valueProviders;
	}

}
