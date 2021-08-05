/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.task;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.util.ClassUtils;

/**
 * A {@link LazyInitializationExcludeFilter} that detects bean methods annotated with
 * {@link Scheduled} or {@link Schedules}.
 *
 * @author Stephane Nicoll
 */
class ScheduledBeanLazyInitializationExcludeFilter implements LazyInitializationExcludeFilter {

	private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));

	ScheduledBeanLazyInitializationExcludeFilter() {
		// Ignore AOP infrastructure such as scoped proxies.
		this.nonAnnotatedClasses.add(AopInfrastructureBean.class);
		this.nonAnnotatedClasses.add(TaskScheduler.class);
		this.nonAnnotatedClasses.add(ScheduledExecutorService.class);
	}

	@Override
	public boolean isExcluded(String beanName, BeanDefinition beanDefinition, Class<?> beanType) {
		return hasScheduledTask(beanType);
	}

	private boolean hasScheduledTask(Class<?> type) {
		Class<?> targetType = ClassUtils.getUserClass(type);
		if (!this.nonAnnotatedClasses.contains(targetType)
				&& AnnotationUtils.isCandidateClass(targetType, Arrays.asList(Scheduled.class, Schedules.class))) {
			Map<Method, Set<Scheduled>> annotatedMethods = MethodIntrospector.selectMethods(targetType,
					(MethodIntrospector.MetadataLookup<Set<Scheduled>>) (method) -> {
						Set<Scheduled> scheduledAnnotations = AnnotatedElementUtils
								.getMergedRepeatableAnnotations(method, Scheduled.class, Schedules.class);
						return (!scheduledAnnotations.isEmpty() ? scheduledAnnotations : null);
					});
			if (annotatedMethods.isEmpty()) {
				this.nonAnnotatedClasses.add(targetType);
			}
			return !annotatedMethods.isEmpty();
		}
		return false;
	}

}
