/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.autoconfigure.jsontest.app;

import com.fasterxml.jackson.annotation.JsonView;
import org.jspecify.annotations.Nullable;

import org.springframework.util.ObjectUtils;

/**
 * Example object to read/write as JSON with view
 *
 * @author Madhura Bhave
 */
public class ExampleJsonObjectWithView {

	@JsonView(TestView.class)
	private @Nullable String value;

	private int id;

	public @Nullable String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		ExampleJsonObjectWithView other = (ExampleJsonObjectWithView) obj;
		return ObjectUtils.nullSafeEquals(this.value, other.value) && ObjectUtils.nullSafeEquals(this.id, other.id);
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public String toString() {
		return this.value + " " + this.id;
	}

	public static class TestView {

	}

}
