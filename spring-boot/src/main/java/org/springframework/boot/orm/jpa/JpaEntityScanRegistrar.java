/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.orm.jpa;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.scan.AbstractEntityScanBeanPostProcessor;
import org.springframework.boot.context.scan.AbstractEntityScanRegistrar;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.util.Assert;

/**
 * {@link ImportBeanDefinitionRegistrar} used by {@link EntityScan}.
 *
 * @author Phillip Webb
 * @author Oliver Gierke
 */
class JpaEntityScanRegistrar extends AbstractEntityScanRegistrar {

	JpaEntityScanRegistrar() {
		super(EntityScan.class, "entityScanBeanPostProcessor",
				JpaEntityScanBeanPostProcessor.class);
	}

	/**
	 * {@link BeanPostProcessor} to set
	 * {@link LocalContainerEntityManagerFactoryBean#setPackagesToScan(String...)} based
	 * on an {@link EntityScan} annotation.
	 */
	static class JpaEntityScanBeanPostProcessor extends
			AbstractEntityScanBeanPostProcessor implements SmartInitializingSingleton {

		private boolean processed;

		JpaEntityScanBeanPostProcessor(String[] packagesToScan) {
			super(packagesToScan);
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof LocalContainerEntityManagerFactoryBean) {
				LocalContainerEntityManagerFactoryBean factoryBean = (LocalContainerEntityManagerFactoryBean) bean;
				factoryBean.setPackagesToScan(getPackagesToScan());
				this.processed = true;
			}
			return bean;
		}

		@Override
		public void afterSingletonsInstantiated() {
			Assert.state(this.processed,
					"Unable to configure "
							+ "LocalContainerEntityManagerFactoryBean from @EntityScan, "
							+ "ensure an appropriate bean is registered.");
		}

	}

}
