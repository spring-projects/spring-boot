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

package org.springframework.boot.web.servlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;

/**
 * {@link BeanFactoryPostProcessor} that registers beans for Servlet components found via
 * package scanning.
 *
 * @see ServletComponentScan
 * @see ServletComponentScanRegistrar
 * @author Andy Wilkinson
 */
class ServletComponentRegisteringPostProcessor implements BeanFactoryPostProcessor,
		ApplicationContextAware {

	private final List<ServletComponentHandler> handlers = Arrays.asList(
			new WebServletHandler(), new WebFilterHandler(), new WebListenerHandler());

	private final Set<String> packagesToScan;

	private ApplicationContext applicationContext;

	public ServletComponentRegisteringPostProcessor(Set<String> packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		if (isRunningInEmbeddedContainer()) {
			ClassPathScanningCandidateComponentProvider componentProvider = createComponentProvider();
			for (String packageToScan : this.packagesToScan) {
				for (BeanDefinition candidate : componentProvider
						.findCandidateComponents(packageToScan)) {
					if (candidate instanceof ScannedGenericBeanDefinition) {
						for (ServletComponentHandler handler : this.handlers) {
							handler.handle(((ScannedGenericBeanDefinition) candidate),
									(BeanDefinitionRegistry) this.applicationContext);
						}
					}
				}
			}
		}
	}

	private boolean isRunningInEmbeddedContainer() {
		return this.applicationContext instanceof EmbeddedWebApplicationContext
				&& ((EmbeddedWebApplicationContext) this.applicationContext)
						.getServletContext() == null;
	}

	private ClassPathScanningCandidateComponentProvider createComponentProvider() {
		ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(
				false);
		for (ServletComponentHandler handler : this.handlers) {
			componentProvider.addIncludeFilter(handler.getTypeFilter());
		}
		return componentProvider;
	}

	Set<String> getPackagesToScan() {
		return Collections.unmodifiableSet(this.packagesToScan);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

}
