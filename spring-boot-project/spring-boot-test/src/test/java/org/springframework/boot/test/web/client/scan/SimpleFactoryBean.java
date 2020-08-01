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
package org.springframework.boot.test.web.client.scan;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * A simple factory bean with no generics. Used to test early initialization doesn't
 * occur.
 *
 * @author Madhura Bhave
 */
@Component
@SuppressWarnings("rawtypes")
public class SimpleFactoryBean implements FactoryBean {

	private static boolean isInitializedEarly = false;

	public SimpleFactoryBean() {
		isInitializedEarly = true;
		throw new RuntimeException();
	}

	@Autowired
	public SimpleFactoryBean(ApplicationContext context) {
		if (isInitializedEarly) {
			throw new RuntimeException();
		}
	}

	@Override
	public Object getObject() {
		return new Object();
	}

	@Override
	public Class<?> getObjectType() {
		return Object.class;
	}

}
