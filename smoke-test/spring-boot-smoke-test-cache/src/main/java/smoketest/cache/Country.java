/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.cache;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

@SuppressWarnings("serial")
public class Country implements Serializable {

	private final String code;

	public Country(String code) {
		this.code = code;
	}

	public String getCode() {
		return this.code;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Country country = (Country) o;

		return this.code.equals(country.code);
	}

	@Override
	public int hashCode() {
		return this.code.hashCode();
	}

}
