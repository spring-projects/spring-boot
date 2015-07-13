package org.springframework.boot.configurationmetadata;

/**
 * Hint for a value a given property may have. Provide the value and
 * an optional description.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class ValueHint {

	private Object value;

	private String description;

	private String shortDescription;

	/**
	 * Return the hint value.
	 */
	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * A description of this value, if any. Can be multi-lines.
	 * @see #getShortDescription()
	 */
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * A single-line, single-sentence description of this hint, if any.
	 * @see #getDescription()
	 */
	public String getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	@Override
	public String toString() {
		return "ValueHint{" + "value=" + this.value + ", description='"
				+ this.description + '\'' + '}';
	}
}
