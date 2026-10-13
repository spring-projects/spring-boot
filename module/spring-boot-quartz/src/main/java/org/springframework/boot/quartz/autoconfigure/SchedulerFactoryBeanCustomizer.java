/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.quartz.autoconfigure;

import javax.sql.DataSource;

import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * Callback interface that can be implemented by beans wishing to customize the Quartz
 * {@link SchedulerFactoryBean} to fine-tune its auto-configuration.
 * <p>
 * For customization of the {@link DataSource} used by Quartz, use of
 * {@link QuartzDataSource @QuartzDataSource} is preferred. It will ensure consistent
 * customization of both the {@link SchedulerFactoryBean} and the
 * {@link QuartzDataSourceScriptDatabaseInitializer}.
 *
 * @author Vedran Pavic
 * @since 4.0.0
 */
@FunctionalInterface
public interface SchedulerFactoryBeanCustomizer {

	/**
	 * Customize the {@link SchedulerFactoryBean}.
	 * @param schedulerFactoryBean the scheduler to customize
	 */
	void customize(SchedulerFactoryBean schedulerFactoryBean);

}
