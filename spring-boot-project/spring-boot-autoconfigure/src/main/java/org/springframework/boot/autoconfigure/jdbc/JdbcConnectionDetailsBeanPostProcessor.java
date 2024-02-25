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

package org.springframework.boot.autoconfigure.jdbc;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

/**
 * Abstract base class for DataSource bean post processors which apply values from
 * {@link JdbcConnectionDetails}. Property-based connection details
 * ({@link PropertiesJdbcConnectionDetails} are ignored as the expectation is that they
 * will have already been applied by configuration property binding. Acts on beans named
 * 'dataSource' of type {@code T}.
 *
 * @param <T> type of the datasource
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
abstract class JdbcConnectionDetailsBeanPostProcessor<T> implements BeanPostProcessor, PriorityOrdered {

	private final Class<T> dataSourceClass;

	private final ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider;

	/**
     * Constructs a new JdbcConnectionDetailsBeanPostProcessor with the specified dataSourceClass and connectionDetailsProvider.
     * 
     * @param dataSourceClass the class of the data source to be used
     * @param connectionDetailsProvider the provider for obtaining the JDBC connection details
     */
    JdbcConnectionDetailsBeanPostProcessor(Class<T> dataSourceClass,
			ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
		this.dataSourceClass = dataSourceClass;
		this.connectionDetailsProvider = connectionDetailsProvider;
	}

	/**
     * This method is called before the initialization of a bean. It checks if the bean is an instance of the dataSourceClass
     * and if the beanName is "dataSource". If both conditions are met, it retrieves the connection details from the connectionDetailsProvider
     * and if the connectionDetails is not an instance of PropertiesJdbcConnectionDetails, it processes the dataSource bean using the connectionDetails.
     * 
     * @param bean the bean object being processed
     * @param beanName the name of the bean being processed
     * @return the processed bean object or the original bean object if no processing is required
     * @throws BeansException if an error occurs during the bean processing
     */
    @Override
	@SuppressWarnings("unchecked")
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (this.dataSourceClass.isAssignableFrom(bean.getClass()) && "dataSource".equals(beanName)) {
			JdbcConnectionDetails connectionDetails = this.connectionDetailsProvider.getObject();
			if (!(connectionDetails instanceof PropertiesJdbcConnectionDetails)) {
				return processDataSource((T) bean, connectionDetails);
			}
		}
		return bean;
	}

	/**
     * Processes the given data source using the provided JDBC connection details.
     *
     * @param dataSource        the data source to be processed
     * @param connectionDetails the JDBC connection details to be used
     * @return the processed object
     */
    protected abstract Object processDataSource(T dataSource, JdbcConnectionDetails connectionDetails);

	/**
     * Returns the order in which this bean post-processor should be executed.
     * This method runs after the ConfigurationPropertiesBindingPostProcessor.
     * The order is determined by adding 2 to the highest precedence value.
     *
     * @return the order of execution for this bean post-processor
     */
    @Override
	public int getOrder() {
		// Runs after ConfigurationPropertiesBindingPostProcessor
		return Ordered.HIGHEST_PRECEDENCE + 2;
	}

}
