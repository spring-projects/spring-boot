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

import java.util.Locale;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Hibernate {@link PhysicalNamingStrategy} that follows Spring recommended naming
 * conventions.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public class SpringPhysicalNamingStrategy implements PhysicalNamingStrategy {

	@Override
	public Identifier toPhysicalCatalogName(Identifier name,
			JdbcEnvironment jdbcEnvironment) {
		return apply(name);
	}

	@Override
	public Identifier toPhysicalSchemaName(Identifier name,
			JdbcEnvironment jdbcEnvironment) {
		return apply(name);
	}

	@Override
	public Identifier toPhysicalTableName(Identifier name,
			JdbcEnvironment jdbcEnvironment) {
		return apply(name);
	}

	@Override
	public Identifier toPhysicalSequenceName(Identifier name,
			JdbcEnvironment jdbcEnvironment) {
		return apply(name);
	}

	@Override
	public Identifier toPhysicalColumnName(Identifier name,
			JdbcEnvironment jdbcEnvironment) {
		return apply(name);
	}

	private Identifier apply(Identifier name) {
		if (name == null) {
			return null;
		}
		StringBuilder text = new StringBuilder(name.getText().replace('.', '_'));
		for (int i = 1; i < text.length() - 1; i++) {
			if (isUnderscoreRequired(text.charAt(i - 1), text.charAt(i), text.charAt(i + 1))) {
				text.insert(i++, '_');
			}
		}
		return new Identifier(text.toString().toLowerCase(Locale.ROOT), name.isQuoted());
	}

	private boolean isUnderscoreRequired(char before, char current, char after) {
		return Character.isLowerCase(before) && Character.isUpperCase(current)
				&& Character.isLowerCase(after);
	}

}
