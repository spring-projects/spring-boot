/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.http.client;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.http.client.ReactorResourceFactory;

/**
 * {@link BeanPostProcessor} to disable the use of global resources in
 * {@link ReactorResourceFactory} preventing test cleanup issues.
 *
 * @author Phillip Webb
 */
class DisableReactorResourceFactoryGlobalResourcesBeanPostProcessor implements BeanPostProcessor {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof ReactorResourceFactory reactorResourceFactory) {
			reactorResourceFactory.setUseGlobalResources(false);
		}
		return bean;
	}

}
