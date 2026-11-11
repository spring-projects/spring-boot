/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.bind;

/**
 * Internal utility to help when dealing with data object property names.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.2.3
 * @see DataObjectBinder
 */
public abstract class DataObjectPropertyName {

	private DataObjectPropertyName() {
	}

	/**
	 * Return the specified Java Bean property name in dashed form.
	 * @param name the source name
	 * @return the dashed from
	 */
	public static String toDashedForm(String name) {
		StringBuilder result = new StringBuilder(name.length());
		boolean inIndex = false;
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (inIndex) {
				result.append(ch);
				if (ch == ']') {
					inIndex = false;
				}
			}
			else {
				if (ch == '[') {
					inIndex = true;
					result.append(ch);
				}
				else {
					ch = (ch != '_') ? ch : '-';
					if (Character.isUpperCase(ch) && !result.isEmpty() && result.charAt(result.length() - 1) != '-') {
						result.append('-');
					}
					result.append(Character.toLowerCase(ch));
				}
			}
		}
		return result.toString();
	}

}
