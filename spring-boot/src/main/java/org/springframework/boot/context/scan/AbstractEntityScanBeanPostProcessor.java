/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.scan;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

/**
 * A base {@link BeanPostProcessor} implementation that holds the packages to use for a
 * given component. An implementation must implement
 * {@link #postProcessBeforeInitialization(Object, String)} and update the component
 * responsible to manage the packages to scan.
 *
 * @author Phillip Webb
 * @author Oliver Gierke
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public abstract class AbstractEntityScanBeanPostProcessor
		implements BeanPostProcessor, Ordered {

	private final String[] packagesToScan;

	protected AbstractEntityScanBeanPostProcessor(String[] packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	/**
	 * Return the packages to use.
	 * @return the packages to use.
	 */
	protected String[] getPackagesToScan() {
		return this.packagesToScan;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public int getOrder() {
		return 0;
	}

}
