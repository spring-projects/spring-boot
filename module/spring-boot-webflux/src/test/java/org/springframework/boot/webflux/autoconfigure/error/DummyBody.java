/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.autoconfigure.error;

import jakarta.validation.constraints.NotNull;

public class DummyBody {

	@NotNull
	@SuppressWarnings("NullAway.Init")
	private String content;

	public String getContent() {
		return this.content;
	}

	public void setContent(String content) {
		this.content = content;
	}

}
