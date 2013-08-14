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

package org.springframework.boot.context.initializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationContextInitializer} that delegates to other initializers that are
 * specified under a {@literal context.initializer.classes} environment property.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
public class EnvironmentDelegateApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	// NOTE: Similar to org.springframework.web.context.ContextLoader

	private static final String PROPERTY_NAME = "context.initializer.classes";

	private int order = 0;

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		ConfigurableEnvironment env = applicationContext.getEnvironment();
		List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> initializerClasses = determineContextInitializerClasses(env);

		if (initializerClasses.size() == 0) {
			// no ApplicationContextInitializers have been declared -> nothing to do
			return;
		}

		Class<?> contextClass = applicationContext.getClass();
		ArrayList<ApplicationContextInitializer<ConfigurableApplicationContext>> initializerInstances = new ArrayList<ApplicationContextInitializer<ConfigurableApplicationContext>>();

		for (Class<ApplicationContextInitializer<ConfigurableApplicationContext>> initializerClass : initializerClasses) {
			Class<?> initializerContextClass = GenericTypeResolver.resolveTypeArgument(
					initializerClass, ApplicationContextInitializer.class);
			Assert.isAssignable(
					initializerContextClass,
					contextClass,
					String.format(
							"Could not add context initializer [%s] as its generic parameter [%s] "
									+ "is not assignable from the type of application context used by this "
									+ "context loader [%s]: ",
							initializerClass.getName(),
							initializerContextClass.getName(), contextClass.getName()));
			initializerInstances.add(BeanUtils.instantiateClass(initializerClass));
		}

		Collections.sort(initializerInstances, new AnnotationAwareOrderComparator());
		for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : initializerInstances) {
			initializer.initialize(applicationContext);
		}
	}

	@SuppressWarnings("unchecked")
	private List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> determineContextInitializerClasses(
			ConfigurableEnvironment env) {
		String classNames = env.getProperty(PROPERTY_NAME);
		List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> classes = new ArrayList<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>>();
		if (StringUtils.hasLength(classNames)) {
			for (String className : StringUtils.tokenizeToStringArray(classNames, ",")) {
				try {
					Class<?> clazz = ClassUtils.forName(className,
							ClassUtils.getDefaultClassLoader());
					Assert.isAssignable(ApplicationContextInitializer.class, clazz,
							"class [" + className
									+ "] must implement ApplicationContextInitializer");
					classes.add((Class<ApplicationContextInitializer<ConfigurableApplicationContext>>) clazz);
				}
				catch (ClassNotFoundException ex) {
					throw new ApplicationContextException(
							"Failed to load context initializer class [" + className
									+ "]", ex);
				}
			}
		}
		return classes;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}
}
