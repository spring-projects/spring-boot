/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.immutable;

import java.util.Comparator;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestConstructorBinding;
import org.springframework.boot.configurationsample.TestDefaultValue;

/**
 * Simple properties, in immutable format.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("immutable")
public class ImmutableSimpleProperties {

	/**
	 * The name of this simple properties.
	 */
	private final String theName;

	/**
	 * A simple flag.
	 */
	private final boolean flag;

	// An interface can still be injected because it might have a converter
	private final Comparator<?> comparator;

	// Even if it is not exposed, we're still offering a way to bind the value through the
	// constructor, so it should be present in the metadata
	@SuppressWarnings("unused")
	private final Long counter;

	@TestConstructorBinding
	public ImmutableSimpleProperties(@TestDefaultValue("boot") String theName, boolean flag, Comparator<?> comparator,
			Long counter) {
		this.theName = theName;
		this.flag = flag;
		this.comparator = comparator;
		this.counter = counter;
	}

	public String getTheName() {
		return this.theName;
	}

	@Deprecated
	public boolean isFlag() {
		return this.flag;
	}

	public Comparator<?> getComparator() {
		return this.comparator;
	}

}
