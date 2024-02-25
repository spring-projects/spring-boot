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

package org.springframework.boot.web.servlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link BeanFactoryPostProcessor} that registers beans for Servlet components found via
 * package scanning.
 *
 * @author Andy Wilkinson
 * @see ServletComponentScan
 * @see ServletComponentScanRegistrar
 */
class ServletComponentRegisteringPostProcessor
		implements BeanFactoryPostProcessor, ApplicationContextAware, BeanFactoryInitializationAotProcessor {

	private static final List<ServletComponentHandler> HANDLERS;

	static {
		List<ServletComponentHandler> servletComponentHandlers = new ArrayList<>();
		servletComponentHandlers.add(new WebServletHandler());
		servletComponentHandlers.add(new WebFilterHandler());
		servletComponentHandlers.add(new WebListenerHandler());
		HANDLERS = Collections.unmodifiableList(servletComponentHandlers);
	}

	private final Set<String> packagesToScan;

	private ApplicationContext applicationContext;

	/**
     * Constructor for ServletComponentRegisteringPostProcessor class.
     * 
     * @param packagesToScan a set of packages to be scanned for servlet components
     */
    ServletComponentRegisteringPostProcessor(Set<String> packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	/**
     * This method is called to post-process the bean factory after it has been initialized.
     * It scans the specified packages for components to register with the embedded web server.
     * 
     * @param beanFactory The bean factory to post-process.
     * @throws BeansException If an error occurs during the post-processing.
     */
    @Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (isRunningInEmbeddedWebServer()) {
			ClassPathScanningCandidateComponentProvider componentProvider = createComponentProvider();
			for (String packageToScan : this.packagesToScan) {
				scanPackage(componentProvider, packageToScan);
			}
		}
	}

	/**
     * Scans the specified package for candidate components using the given component provider.
     * For each candidate component found, if it is an annotated bean definition, it is passed to
     * each handler in the HANDLERS list for further processing.
     *
     * @param componentProvider the component provider used for scanning
     * @param packageToScan the package to scan for candidate components
     */
    private void scanPackage(ClassPathScanningCandidateComponentProvider componentProvider, String packageToScan) {
		for (BeanDefinition candidate : componentProvider.findCandidateComponents(packageToScan)) {
			if (candidate instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
				for (ServletComponentHandler handler : HANDLERS) {
					handler.handle(annotatedBeanDefinition, (BeanDefinitionRegistry) this.applicationContext);
				}
			}
		}
	}

	/**
     * Checks if the application is running in an embedded web server.
     * 
     * @return true if the application is running in an embedded web server, false otherwise
     */
    private boolean isRunningInEmbeddedWebServer() {
		return this.applicationContext instanceof WebApplicationContext webApplicationContext
				&& webApplicationContext.getServletContext() == null;
	}

	/**
     * Creates a new instance of ClassPathScanningCandidateComponentProvider and configures it with the necessary settings.
     * 
     * @return The configured ClassPathScanningCandidateComponentProvider instance.
     */
    private ClassPathScanningCandidateComponentProvider createComponentProvider() {
		ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(
				false);
		componentProvider.setEnvironment(this.applicationContext.getEnvironment());
		componentProvider.setResourceLoader(this.applicationContext);
		for (ServletComponentHandler handler : HANDLERS) {
			componentProvider.addIncludeFilter(handler.getTypeFilter());
		}
		return componentProvider;
	}

	/**
     * Returns an unmodifiable set of packages to scan.
     * 
     * @return an unmodifiable set of packages to scan
     */
    Set<String> getPackagesToScan() {
		return Collections.unmodifiableSet(this.packagesToScan);
	}

	/**
     * Sets the application context for this ServletComponentRegisteringPostProcessor.
     * 
     * @param applicationContext the application context to be set
     * @throws BeansException if an error occurs while setting the application context
     */
    @Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
     * Process the bean factory ahead of time to contribute to the AOT initialization.
     * 
     * @param beanFactory the configurable listable bean factory
     * @return the bean factory initialization AOT contribution
     */
    @Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		return new BeanFactoryInitializationAotContribution() {

			@Override
			public void applyTo(GenerationContext generationContext,
					BeanFactoryInitializationCode beanFactoryInitializationCode) {
				for (String beanName : beanFactory.getBeanDefinitionNames()) {
					BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
					if (Objects.equals(definition.getBeanClassName(),
							WebListenerHandler.ServletComponentWebListenerRegistrar.class.getName())) {
						String listenerClassName = (String) definition.getConstructorArgumentValues()
							.getArgumentValue(0, String.class)
							.getValue();
						generationContext.getRuntimeHints()
							.reflection()
							.registerType(TypeReference.of(listenerClassName),
									MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
					}
				}
			}

		};
	}

}
