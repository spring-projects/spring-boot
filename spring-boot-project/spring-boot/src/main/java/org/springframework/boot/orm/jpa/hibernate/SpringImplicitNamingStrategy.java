/*
 * Copyright 2012-2017 the original author or authors.
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

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitJoinTableNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;

/**
 * Hibernate {@link ImplicitNamingStrategy} that follows Spring recommended naming
 * conventions. Naming conventions implemented here are identical to
 * {@link ImplicitNamingStrategyJpaCompliantImpl} with the exception that join table names
 * are of the form
 * <code>{owning_physical_table_name}_{association_owning_property_name}</code>.
 *
 * @author Andy Wilkinson
 * @since 1.4.0
 */
public class SpringImplicitNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {

	@Override
	public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
		String name = source.getOwningPhysicalTableName() + "_"
				+ source.getAssociationOwningAttributePath().getProperty();
		return toIdentifier(name, source.getBuildingContext());
	}

}
