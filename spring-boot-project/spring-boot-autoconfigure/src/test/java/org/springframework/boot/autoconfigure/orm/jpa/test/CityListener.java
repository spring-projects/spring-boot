/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.orm.jpa.test;

import javax.persistence.PostLoad;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

public class CityListener {

	private ConfigurableBeanFactory beanFactory;

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
