/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import ch.qos.logback.core.model.NamedModel;

/**
 * Logback {@link NamedModel model} to support {@code <springProperty>} tags. Allows
 * Logback properties to be sourced from the Spring environment.
 *
 * @author Andy Wilkinson
 * @see SpringPropertyAction
 * @see SpringPropertyModelHandler
 */
class SpringPropertyModel extends NamedModel {

	@SuppressWarnings("NullAway.Init")
	private String scope;

	@SuppressWarnings("NullAway.Init")
	private String defaultValue;

	@SuppressWarnings("NullAway.Init")
	private String source;

	String getScope() {
		return this.scope;
	}

	void setScope(String scope) {
		this.scope = scope;
	}

	String getDefaultValue() {
		return this.defaultValue;
	}

	void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	String getSource() {
		return this.source;
	}

	void setSource(String source) {
		this.source = source;
	}

}
