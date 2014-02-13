/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.context.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationListener} that delegates to other listeners that are specified under
 * a {@literal context.listener.classes} environment property.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
public class DelegatingApplicationListener implements
		ApplicationListener<ApplicationEvent>, Ordered {

	// NOTE: Similar to org.springframework.web.context.ContextLoader

	private static final String PROPERTY_NAME = "context.listener.classes";

	private int order = 0;

	private SimpleApplicationEventMulticaster multicaster;

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationEnvironmentPreparedEvent) {
			List<ApplicationListener<ApplicationEvent>> delegates = getListeners(((ApplicationEnvironmentPreparedEvent) event)
					.getEnvironment());
			if (delegates.isEmpty()) {
				return;
			}
			this.multicaster = new SimpleApplicationEventMulticaster();
			for (ApplicationListener<ApplicationEvent> listener : delegates) {
				this.multicaster.addApplicationListener(listener);
			}
		}
		if (this.multicaster != null) {
			this.multicaster.multicastEvent(event);
		}
	}

	@SuppressWarnings("unchecked")
	private List<ApplicationListener<ApplicationEvent>> getListeners(
			ConfigurableEnvironment env) {
		String classNames = env.getProperty(PROPERTY_NAME);
		List<ApplicationListener<ApplicationEvent>> listeners = new ArrayList<ApplicationListener<ApplicationEvent>>();
		if (StringUtils.hasLength(classNames)) {
			for (String className : StringUtils.commaDelimitedListToSet(classNames)) {
				try {
					Class<?> clazz = ClassUtils.forName(className,
							ClassUtils.getDefaultClassLoader());
					Assert.isAssignable(ApplicationListener.class, clazz, "class ["
							+ className + "] must implement ApplicationListener");
					listeners.add((ApplicationListener<ApplicationEvent>) BeanUtils
							.instantiateClass(clazz));
				}
				catch (Exception ex) {
					throw new ApplicationContextException(
							"Failed to load context listener class [" + className + "]",
							ex);
				}
			}
		}
		AnnotationAwareOrderComparator.sort(listeners);
		return listeners;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

}
