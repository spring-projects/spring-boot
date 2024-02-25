/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * An {@link OperationArgumentResolver} for {@link Producible producible enums}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.5.0
 */
public class ProducibleOperationArgumentResolver implements OperationArgumentResolver {

	private final Supplier<List<String>> accepts;

	/**
	 * Create a new {@link ProducibleOperationArgumentResolver} instance.
	 * @param accepts supplier that returns accepted mime types
	 */
	public ProducibleOperationArgumentResolver(Supplier<List<String>> accepts) {
		this.accepts = accepts;
	}

	/**
     * Determines if the given type can be resolved by this argument resolver.
     * 
     * @param type the type to check
     * @return true if the type can be resolved, false otherwise
     */
    @Override
	public boolean canResolve(Class<?> type) {
		return Producible.class.isAssignableFrom(type) && Enum.class.isAssignableFrom(type);
	}

	/**
     * Resolves the given type by returning an instance of the specified class.
     * 
     * @param type the class type to resolve
     * @return an instance of the specified class
     * @throws ClassCastException if the resolved object cannot be cast to the specified class
     */
    @SuppressWarnings("unchecked")
	@Override
	public <T> T resolve(Class<T> type) {
		return (T) resolveProducible((Class<Enum<? extends Producible<?>>>) type);
	}

	/**
     * Resolves the producible value for the given type.
     * 
     * @param type the type of producible
     * @return the resolved producible value
     */
    private Enum<? extends Producible<?>> resolveProducible(Class<Enum<? extends Producible<?>>> type) {
		List<String> accepts = this.accepts.get();
		List<Enum<? extends Producible<?>>> values = getValues(type);
		if (CollectionUtils.isEmpty(accepts)) {
			return getDefaultValue(values);
		}
		Enum<? extends Producible<?>> result = null;
		for (String accept : accepts) {
			for (String mimeType : MimeTypeUtils.tokenize(accept)) {
				result = mostRecent(result, forMimeType(values, MimeTypeUtils.parseMimeType(mimeType)));
			}
		}
		return result;
	}

	/**
     * Returns the most recent Enum value between the existing and candidate Enum values.
     * 
     * @param existing The existing Enum value.
     * @param candidate The candidate Enum value.
     * @return The most recent Enum value.
     */
    private Enum<? extends Producible<?>> mostRecent(Enum<? extends Producible<?>> existing,
			Enum<? extends Producible<?>> candidate) {
		int existingOrdinal = (existing != null) ? existing.ordinal() : -1;
		int candidateOrdinal = (candidate != null) ? candidate.ordinal() : -1;
		return (candidateOrdinal > existingOrdinal) ? candidate : existing;
	}

	/**
     * Returns the Enum value that corresponds to the given MimeType, based on the list of values and their associated MimeType.
     * If the MimeType is a wildcard type and a wildcard subtype, the default value is returned.
     * Otherwise, it iterates through the list of values and checks if the MimeType is compatible with the produced MimeType of each value.
     * If a compatible value is found, it is returned.
     * If no compatible value is found, null is returned.
     *
     * @param values   the list of Enum values with associated MimeType
     * @param mimeType the MimeType to match against the values
     * @return the Enum value that corresponds to the given MimeType, or null if no match is found
     */
    private Enum<? extends Producible<?>> forMimeType(List<Enum<? extends Producible<?>>> values, MimeType mimeType) {
		if (mimeType.isWildcardType() && mimeType.isWildcardSubtype()) {
			return getDefaultValue(values);
		}
		for (Enum<? extends Producible<?>> candidate : values) {
			if (mimeType.isCompatibleWith(((Producible<?>) candidate).getProducedMimeType())) {
				return candidate;
			}
		}
		return null;
	}

	/**
     * Retrieves a list of values for the given type.
     * 
     * @param type the type of the values to retrieve
     * @return a list of values for the given type
     * @throws IllegalStateException if multiple default values are declared in the given type
     */
    private List<Enum<? extends Producible<?>>> getValues(Class<Enum<? extends Producible<?>>> type) {
		List<Enum<? extends Producible<?>>> values = Arrays.asList(type.getEnumConstants());
		Collections.reverse(values);
		Assert.state(values.stream().filter(this::isDefault).count() <= 1,
				"Multiple default values declared in " + type.getName());
		return values;
	}

	/**
     * Returns the default value from the given list of producible values.
     * 
     * @param values the list of producible values
     * @return the default value from the list, or the first value if no default is found
     */
    private Enum<? extends Producible<?>> getDefaultValue(List<Enum<? extends Producible<?>>> values) {
		return values.stream().filter(this::isDefault).findFirst().orElseGet(() -> values.get(0));
	}

	/**
     * Checks if the given value is the default value of the specified Producible object.
     * 
     * @param value the value to be checked
     * @return true if the value is the default value, false otherwise
     */
    private boolean isDefault(Enum<? extends Producible<?>> value) {
		return ((Producible<?>) value).isDefault();
	}

}
