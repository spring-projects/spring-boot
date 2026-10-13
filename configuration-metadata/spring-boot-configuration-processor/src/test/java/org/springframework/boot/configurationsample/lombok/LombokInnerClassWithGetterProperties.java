/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.lombok;

import lombok.Data;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

@Data
@TestConfigurationProperties("config")
@SuppressWarnings("unused")
public class LombokInnerClassWithGetterProperties {

	private final Foo first = new Foo();

	public Foo getFirst() {
		return this.first;
	}

	@Data
	public static class Foo {

		private String name;

	}

}
