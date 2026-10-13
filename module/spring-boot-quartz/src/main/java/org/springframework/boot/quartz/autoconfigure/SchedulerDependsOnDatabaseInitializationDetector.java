/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.quartz.autoconfigure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.quartz.Scheduler;

import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDependsOnDatabaseInitializationDetector;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitializationDetector;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * A {@link DependsOnDatabaseInitializationDetector} for Quartz {@link Scheduler} and
 * {@link SchedulerFactoryBean}.
 *
 * @author Andy Wilkinson
 */
class SchedulerDependsOnDatabaseInitializationDetector
		extends AbstractBeansOfTypeDependsOnDatabaseInitializationDetector {

	@Override
	protected Set<Class<?>> getDependsOnDatabaseInitializationBeanTypes() {
		return new HashSet<>(Arrays.asList(Scheduler.class, SchedulerFactoryBean.class));
	}

}
