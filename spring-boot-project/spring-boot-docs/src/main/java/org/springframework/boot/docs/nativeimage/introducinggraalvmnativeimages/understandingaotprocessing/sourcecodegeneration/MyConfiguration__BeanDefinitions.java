/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docs.nativeimage.introducinggraalvmnativeimages.understandingaotprocessing.sourcecodegeneration;

import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link MyConfiguration}.
 */
@SuppressWarnings("javadoc")
public class MyConfiguration__BeanDefinitions {

	/**
	 * Get the bean definition for 'myConfiguration'.
	 */
	public static BeanDefinition getMyConfigurationBeanDefinition() {
		Class<?> beanType = MyConfiguration.class;
		RootBeanDefinition beanDefinition = new RootBeanDefinition(beanType);
		beanDefinition.setInstanceSupplier(MyConfiguration::new);
		return beanDefinition;
	}

	/**
	 * Get the bean instance supplier for 'myBean'.
	 */
	private static BeanInstanceSupplier<MyBean> getMyBeanInstanceSupplier() {
		return BeanInstanceSupplier.<MyBean>forFactoryMethod(MyConfiguration.class, "myBean")
			.withGenerator((registeredBean) -> registeredBean.getBeanFactory().getBean(MyConfiguration.class).myBean());
	}

	/**
	 * Get the bean definition for 'myBean'.
	 */
	public static BeanDefinition getMyBeanBeanDefinition() {
		Class<?> beanType = MyBean.class;
		RootBeanDefinition beanDefinition = new RootBeanDefinition(beanType);
		beanDefinition.setInstanceSupplier(getMyBeanInstanceSupplier());
		return beanDefinition;
	}

}
