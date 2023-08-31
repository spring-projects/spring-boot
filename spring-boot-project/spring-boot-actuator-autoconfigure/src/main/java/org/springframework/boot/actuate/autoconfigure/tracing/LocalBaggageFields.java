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

package org.springframework.boot.actuate.autoconfigure.tracing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import brave.baggage.BaggagePropagation;
import brave.baggage.BaggagePropagationConfig;
import brave.baggage.BaggagePropagationConfig.SingleBaggageField;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Local baggage fields.
 *
 * @author Moritz Halbritter
 */
class LocalBaggageFields {

	private final List<String> fields;

	LocalBaggageFields(List<String> fields) {
		Assert.notNull(fields, "fields must not be null");
		this.fields = fields;
	}

	/**
	 * Returns the local fields as a list.
	 * @return the list
	 */
	List<String> asList() {
		return Collections.unmodifiableList(this.fields);
	}

	/**
	 * Extracts the local fields from the given propagation factory builder.
	 * @param builder the propagation factory builder to extract the local fields from
	 * @return the local fields
	 */
	static LocalBaggageFields extractFrom(BaggagePropagation.FactoryBuilder builder) {
		List<String> localFields = new ArrayList<>();
		for (BaggagePropagationConfig config : builder.configs()) {
			if (config instanceof SingleBaggageField field) {
				if (CollectionUtils.isEmpty(field.keyNames())) {
					localFields.add(field.field().name());
				}
			}
		}
		return new LocalBaggageFields(localFields);
	}

	/**
	 * Creates empty local fields.
	 * @return the empty local fields
	 */
	static LocalBaggageFields empty() {
		return new LocalBaggageFields(Collections.emptyList());
	}

}
