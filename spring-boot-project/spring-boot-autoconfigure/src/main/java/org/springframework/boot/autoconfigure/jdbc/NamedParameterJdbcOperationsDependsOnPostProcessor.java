/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * {@link BeanFactoryPostProcessor} that can be used to dynamically declare that all
 * {@link NamedParameterJdbcOperations} beans should "depend on" one or more specific
 * beans.
 *
 * @author Dan Zheng
 * @since 2.1.4
 * @see BeanDefinition#setDependsOn(String[])
 */
public class NamedParameterJdbcOperationsDependsOnPostProcessor
		extends AbstractDependsOnBeanFactoryPostProcessor {

	public NamedParameterJdbcOperationsDependsOnPostProcessor(String... dependsOn) {
		super(NamedParameterJdbcOperations.class, dependsOn);
	}

}
