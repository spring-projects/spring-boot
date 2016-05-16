/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.orm.jpa.hibernate;

import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.internal.util.StringHelper;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Hibernate {@link org.hibernate.cfg.NamingStrategy} that follows Spring recommended
 * naming conventions. Naming conventions implemented here are identical to
 * {@link ImprovedNamingStrategy} with the exception that foreign key columns include the
 * referenced column name.
 *
 * @author Phillip Webb
 * @see "http://stackoverflow.com/questions/7689206/ejb3namingstrategy-vs-improvednamingstrategy-foreign-key-naming"
 * @since 1.2.0
 */
@SuppressWarnings("serial")
public class SpringNamingStrategy extends ImprovedNamingStrategy {

	@Override
	public String foreignKeyColumnName(String propertyName, String propertyEntityName,
			String propertyTableName, String referencedColumnName) {
		String name = propertyTableName;
		if (propertyName != null) {
			name = StringHelper.unqualify(propertyName);
		}
		Assert.state(StringUtils.hasLength(name),
				"Unable to generate foreignKeyColumnName");
		return columnName(name) + "_" + referencedColumnName;
	}

}
