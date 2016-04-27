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

/**
 * Test implementation of {@link AbstractEntityScanRegistrar}.
 *
 * @author Stephane Nicoll
 */
class TestEntityScanRegistrar extends AbstractEntityScanRegistrar {

	static final String BEAN_NAME = "testEntityScanBeanPostProcessor";

	TestEntityScanRegistrar() {
		super(TestEntityScan.class, BEAN_NAME, TestEntityScanBeanPostProcessor.class);
	}

	static class TestFactoryBean {
		private String[] packagesToScan;

		public void setPackagesToScan(String... packagesToScan) {
			this.packagesToScan = packagesToScan;
		}

		public String[] getPackagesToScan() {
			return this.packagesToScan;
		}

	}

	static class TestEntityScanBeanPostProcessor
			extends AbstractEntityScanBeanPostProcessor {

		TestEntityScanBeanPostProcessor(String[] packagesToScan) {
			super(packagesToScan);
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof TestFactoryBean) {
				TestFactoryBean factoryBean = (TestFactoryBean) bean;
				factoryBean.setPackagesToScan(getPackagesToScan());
			}
			return bean;
		}

	}

}
