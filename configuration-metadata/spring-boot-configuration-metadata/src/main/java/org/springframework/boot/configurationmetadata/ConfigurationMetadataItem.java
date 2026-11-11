/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationmetadata;

/**
 * An extension of {@link ConfigurationMetadataProperty} that provides a reference to its
 * source.
 *
 * @author Stephane Nicoll
 */
class ConfigurationMetadataItem extends ConfigurationMetadataProperty {

	private String sourceType;

	private String sourceMethod;

	/**
	 * The class name of the source that contributed this property. For example, if the
	 * property was from a class annotated with {@code @ConfigurationProperties} this
	 * attribute would contain the fully qualified name of that class.
	 * @return the source type
	 */
	String getSourceType() {
		return this.sourceType;
	}

	void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	/**
	 * The full name of the method (including parenthesis and argument types) that
	 * contributed this property. For example, the name of a getter in a
	 * {@code @ConfigurationProperties} annotated class.
	 * @return the source method
	 */
	String getSourceMethod() {
		return this.sourceMethod;
	}

	void setSourceMethod(String sourceMethod) {
		this.sourceMethod = sourceMethod;
	}

}
