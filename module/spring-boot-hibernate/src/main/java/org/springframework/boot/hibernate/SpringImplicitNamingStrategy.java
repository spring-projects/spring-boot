/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hibernate;

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
 * @since 4.0.0
 */
public class SpringImplicitNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {

	@Override
	public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
		String name = source.getOwningPhysicalTableName() + "_"
				+ source.getAssociationOwningAttributePath().getProperty();
		return toIdentifier(name, source.getBuildingContext());
	}

}
