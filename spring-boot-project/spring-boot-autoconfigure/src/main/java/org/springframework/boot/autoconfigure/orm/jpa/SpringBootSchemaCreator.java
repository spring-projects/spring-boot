/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.orm.jpa;

import org.hibernate.boot.Metadata;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.springframework.boot.autoconfigure.jdbc.DataSourceInitializer;

/**
 * Spring Boot {@link SchemaCreator}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class SpringBootSchemaCreator implements SchemaCreator {

	private static final CoreMessageLogger log = CoreLogging.messageLogger(SpringBootSchemaCreator.class);

	private final HibernateSchemaManagementTool tool;

	private final DataSourceInitializer dataSourceInitializer;

	private final SchemaCreator creator;

	SpringBootSchemaCreator(HibernateSchemaManagementTool tool, SchemaCreator creator,
			DataSourceInitializer dataSourceInitializer) {
		this.tool = tool;
		this.creator = creator;
		this.dataSourceInitializer = dataSourceInitializer;
	}

	@Override
	public void doCreation(Metadata metadata, ExecutionOptions options, SourceDescriptor sourceDescriptor,
			TargetDescriptor targetDescriptor) {
		if (!targetDescriptor.getTargetTypes().contains(TargetType.DATABASE)) {
			this.creator.doCreation(metadata, options, sourceDescriptor, targetDescriptor);
			return;
		}
		GenerationTarget databaseTarget = getDatabaseTarget(options, targetDescriptor);
		databaseTarget.prepare();
		try {
			this.creator.doCreation(metadata, options, sourceDescriptor, targetDescriptor);
			this.dataSourceInitializer.initializeDataSource();
		}
		finally {
			try {
				databaseTarget.release();
			}
			catch (Exception ex) {
				log.debugf("Problem releasing GenerationTarget [%s] : %s", databaseTarget, ex.getMessage());
			}
		}
	}

	private GenerationTarget getDatabaseTarget(ExecutionOptions options, TargetDescriptor targetDescriptor) {
		JdbcContext jdbcContext = this.tool.resolveJdbcContext(options.getConfigurationValues());
		DdlTransactionIsolator ddlTransactionIsolator = this.tool.getDdlTransactionIsolator(jdbcContext);
		return new GenerationTargetToDatabase(ddlTransactionIsolator);
	}

}
