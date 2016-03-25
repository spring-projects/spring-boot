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

package org.springframework.boot.neo4j;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.scan.AbstractEntityScanBeanPostProcessor;
import org.springframework.boot.context.scan.AbstractEntityScanRegistrar;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.util.Assert;

/**
 * {@link ImportBeanDefinitionRegistrar} used by {@link NodeEntityScan}.
 *
 * @author Stephane Nicoll
 */
class NodeEntityScanRegistrar extends AbstractEntityScanRegistrar {

	NodeEntityScanRegistrar() {
		super(NodeEntityScan.class, "nodeEntityScanBeanPostProcessor",
				NodeEntityScanBeanPostProcessor.class);
	}

	/**
	 * {@link BeanPostProcessor} to set
	 * {@link SessionFactoryProvider#setPackagesToScan(String...)} based on an
	 * {@link NodeEntityScan} annotation.
	 */
	static class NodeEntityScanBeanPostProcessor extends
			AbstractEntityScanBeanPostProcessor implements SmartInitializingSingleton {

		private boolean processed;

		NodeEntityScanBeanPostProcessor(String[] packagesToScan) {
			super(packagesToScan);
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof SessionFactoryProvider) {
				SessionFactoryProvider provider = (SessionFactoryProvider) bean;
				provider.setPackagesToScan(getPackagesToScan());
				this.processed = true;
			}
			return bean;
		}

		@Override
		public void afterSingletonsInstantiated() {
			Assert.state(this.processed,
					"Unable to configure "
							+ "SessionFactoryFactoryBean from @NodeEntityScan, "
							+ "ensure an appropriate bean is registered.");
		}

	}

}
