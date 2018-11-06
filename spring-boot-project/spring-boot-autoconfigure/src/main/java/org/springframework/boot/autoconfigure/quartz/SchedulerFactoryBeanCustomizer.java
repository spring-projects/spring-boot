/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.quartz;

import javax.sql.DataSource;

import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * Callback interface that can be implemented by beans wishing to customize the Quartz
 * {@link SchedulerFactoryBean} before it is fully initialized, in particular to tune its
 * configuration.
 * <p>
 * For customization of the {@link DataSource} used by Quartz, use of
 * {@link QuartzDataSource @QuartzDataSource} is preferred. It will ensure consistent
 * customization of both the {@link SchedulerFactoryBean} and the
 * {@link QuartzDataSourceInitializer}.
 *
 * @author Vedran Pavic
 * @since 2.0.0
 */
@FunctionalInterface
public interface SchedulerFactoryBeanCustomizer {

	/**
	 * Customize the {@link SchedulerFactoryBean}.
	 * @param schedulerFactoryBean the scheduler to customize
	 */
	void customize(SchedulerFactoryBean schedulerFactoryBean);

}
