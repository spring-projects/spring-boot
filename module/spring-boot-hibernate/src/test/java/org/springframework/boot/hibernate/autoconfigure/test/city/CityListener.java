/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hibernate.autoconfigure.test.city;

import jakarta.persistence.PostLoad;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

public class CityListener {

	private @Nullable ConfigurableBeanFactory beanFactory;

	public CityListener() {
	}

	@Autowired
	public CityListener(ConfigurableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@PostLoad
	public void postLoad(City city) {
		if (this.beanFactory != null) {
			this.beanFactory.registerSingleton(City.class.getName(), city);
		}
	}

}
