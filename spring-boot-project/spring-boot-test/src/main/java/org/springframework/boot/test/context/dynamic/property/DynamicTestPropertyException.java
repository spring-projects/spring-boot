package org.springframework.boot.test.context.dynamic.property;

/**
 * This exception used in the processing of the {@link DynamicTestProperty}
 *
 * @author Anatoliy Korovin
 */
public class DynamicTestPropertyException extends RuntimeException {

	public DynamicTestPropertyException(String message) {
		super(message);
	}
}
