/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.configurationprocessor;

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.type.TypeMirror;

/**
 * Filter to excluded elements that don't make sense to process.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
class TypeExcludeFilter {

	private final Set<String> excludes = new HashSet<String>();

	public TypeExcludeFilter() {
		add("java.io.Writer");
		add("java.io.PrintWriter");
		add("java.lang.ClassLoader");
		add("java.util.concurrent.ThreadFactory");
		add("javax.sql.DataSource");
		add("com.zaxxer.hikari.IConnectionCustomizer");
		add("groovy.text.markup.MarkupTemplateEngine");
		add("org.apache.tomcat.jdbc.pool.PoolConfiguration");
		add("org.apache.tomcat.jdbc.pool.Validator");
		add("org.flywaydb.core.api.MigrationVersion");
		add("org.flywaydb.core.api.callback.FlywayCallback");
		add("org.flywaydb.core.api.resolver.MigrationResolver");
		add("org.springframework.http.MediaType");
	}

	private void add(String className) {
		this.excludes.add(className);
	}

	public boolean isExcluded(TypeMirror type) {
		if (type == null) {
			return false;
		}
		String typeName = type.toString();
		if (typeName.endsWith("[]")) {
			typeName = typeName.substring(0, typeName.length() - 2);
		}
		return this.excludes.contains(typeName);
	}

}
