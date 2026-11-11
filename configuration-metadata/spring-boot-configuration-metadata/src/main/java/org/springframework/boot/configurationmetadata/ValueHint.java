/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationmetadata;

import java.io.Serializable;

/**
 * Hint for a value a given property may have. Provide the value and an optional
 * description.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@SuppressWarnings("serial")
public class ValueHint implements Serializable {

	private Object value;

	private String description;

	private String shortDescription;

	/**
	 * Return the hint value.
	 * @return the value
	 */
	public Object getValue() {
		return this.value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * A description of this value, if any. Can be multi-lines.
	 * @return the description
	 * @see #getShortDescription()
	 */
	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * A single-line, single-sentence description of this hint, if any.
	 * @return the short description
	 * @see #getDescription()
	 */
	public String getShortDescription() {
		return this.shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	@Override
	public String toString() {
		return "ValueHint{value=" + this.value + ", description='" + this.description + '\'' + '}';
	}

}
