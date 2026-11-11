/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.FactoryBean;

public class EarlyInitFactoryBean implements FactoryBean<String> {

	private @Nullable String propertyFromConfig;

	public void setPropertyFromConfig(String propertyFromConfig) {
		this.propertyFromConfig = propertyFromConfig;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public @Nullable Class<?> getObjectType() {
		return null;
	}

	@Override
	public @Nullable String getObject() throws Exception {
		return this.propertyFromConfig;
	}

}
