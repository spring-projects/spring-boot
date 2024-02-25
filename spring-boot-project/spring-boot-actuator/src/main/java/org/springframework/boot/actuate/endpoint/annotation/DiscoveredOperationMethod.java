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

package org.springframework.boot.actuate.endpoint.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.Producible;
import org.springframework.boot.actuate.endpoint.invoke.reflect.OperationMethod;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.Assert;

/**
 * An {@link OperationMethod} discovered by an {@link EndpointDiscoverer}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class DiscoveredOperationMethod extends OperationMethod {

	private final List<String> producesMediaTypes;

	/**
     * Creates a new DiscoveredOperationMethod object.
     * 
     * @param method              the Method object representing the discovered operation method
     * @param operationType       the OperationType of the discovered operation method
     * @param annotationAttributes the AnnotationAttributes containing the attributes of the discovered operation method
     * @throws IllegalArgumentException if annotationAttributes is null
     */
    public DiscoveredOperationMethod(Method method, OperationType operationType,
			AnnotationAttributes annotationAttributes) {
		super(method, operationType);
		Assert.notNull(annotationAttributes, "AnnotationAttributes must not be null");
		List<String> producesMediaTypes = new ArrayList<>();
		producesMediaTypes.addAll(Arrays.asList(annotationAttributes.getStringArray("produces")));
		producesMediaTypes.addAll(getProducesFromProducable(annotationAttributes));
		this.producesMediaTypes = Collections.unmodifiableList(producesMediaTypes);
	}

	/**
     * Retrieves the list of produces from the given AnnotationAttributes.
     * 
     * @param annotationAttributes the AnnotationAttributes containing the produces information
     * @return the list of produces as strings
     */
    private <E extends Enum<E> & Producible<E>> List<String> getProducesFromProducable(
			AnnotationAttributes annotationAttributes) {
		Class<?> type = getProducesFrom(annotationAttributes);
		if (type == Producible.class) {
			return Collections.emptyList();
		}
		List<String> produces = new ArrayList<>();
		for (Object value : type.getEnumConstants()) {
			produces.add(((Producible<?>) value).getProducedMimeType().toString());
		}
		return produces;
	}

	/**
     * Retrieves the producesFrom value from the given AnnotationAttributes object.
     * 
     * @param annotationAttributes the AnnotationAttributes object containing the producesFrom value
     * @return the producesFrom value if present, otherwise returns the default value Producible.class
     */
    private Class<?> getProducesFrom(AnnotationAttributes annotationAttributes) {
		try {
			return annotationAttributes.getClass("producesFrom");
		}
		catch (IllegalArgumentException ex) {
			return Producible.class;
		}
	}

	/**
     * Returns the list of media types that this method produces.
     *
     * @return the list of media types that this method produces
     */
    public List<String> getProducesMediaTypes() {
		return this.producesMediaTypes;
	}

}
