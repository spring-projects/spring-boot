/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.orm.jpa;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceInitialization.DataSourceInitializedEvent;
import org.springframework.context.ApplicationContext;

public class DataSourceInitializedPublisher implements BeanPostProcessor {

	@Autowired
	private ApplicationContext applicationContext;
	private DataSource dataSource;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		if (bean instanceof DataSource) {
			// Normally this will be the right DataSource
			this.dataSource = (DataSource) bean;
		}
		if (bean instanceof EntityManagerFactory && this.dataSource != null) {
			this.applicationContext.publishEvent(new DataSourceInitializedEvent(
					this.dataSource));
		}
		return bean;
	}
}