/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.jpa.autoconfigure;

import java.util.Map;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration.JpaRepositoriesImportSelector;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.util.ClassUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's JPA Repositories.
 * <p>
 * Activates when there is a bean of type {@link javax.sql.DataSource} configured in the
 * context, the Spring Data JPA {@link JpaRepository} type is on the classpath, and there
 * is no other, existing {@link JpaRepository} configured.
 * <p>
 * Once in effect, the auto-configuration is the equivalent of enabling JPA repositories
 * using the {@link EnableJpaRepositories @EnableJpaRepositories} annotation.
 * <p>
 * In case {@link EnableEnversRepositories} is on the classpath,
 * {@link EnversRevisionRepositoryFactoryBean} is used instead of
 * {@link JpaRepositoryFactoryBean} to support {@link RevisionRepository} with Hibernate
 * Envers.
 * <p>
 * This configuration class will activate <em>after</em> the Hibernate auto-configuration.
 *
 * @author Phillip Webb
 * @author Josh Long
 * @author Scott Frederick
 * @author Stefano Cordio
 * @since 4.0.0
 * @see EnableJpaRepositories
 */
@AutoConfiguration(after = { HibernateJpaAutoConfiguration.class, TaskExecutionAutoConfiguration.class })
@ConditionalOnBean(DataSource.class)
@ConditionalOnClass(JpaRepository.class)
@ConditionalOnMissingBean({ JpaRepositoryFactoryBean.class, JpaRepositoryConfigExtension.class })
@ConditionalOnBooleanProperty(name = "spring.data.jpa.repositories.enabled", matchIfMissing = true)
@Import(JpaRepositoriesImportSelector.class)
public final class DataJpaRepositoriesAutoConfiguration {

	@Bean
	@Conditional(BootstrapExecutorCondition.class)
	EntityManagerFactoryBuilderCustomizer entityManagerFactoryBootstrapExecutorCustomizer(
			Map<String, AsyncTaskExecutor> taskExecutors) {
		return (builder) -> {
			AsyncTaskExecutor bootstrapExecutor = determineBootstrapExecutor(taskExecutors);
			if (bootstrapExecutor != null) {
				builder.setBootstrapExecutor(bootstrapExecutor);
			}
		};
	}

	@Bean
	static LazyInitializationExcludeFilter eagerJpaMetamodelCacheCleanup() {
		return (name, definition, type) -> "org.springframework.data.jpa.util.JpaMetamodelCacheCleanup".equals(name);
	}

	private @Nullable AsyncTaskExecutor determineBootstrapExecutor(Map<String, AsyncTaskExecutor> taskExecutors) {
		if (taskExecutors.size() == 1) {
			return taskExecutors.values().iterator().next();
		}
		return taskExecutors.get(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME);
	}

	private static final class BootstrapExecutorCondition extends AnyNestedCondition {

		BootstrapExecutorCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(name = "spring.data.jpa.repositories.bootstrap-mode", havingValue = "deferred")
		static class DeferredBootstrapMode {

		}

		@ConditionalOnProperty(name = "spring.data.jpa.repositories.bootstrap-mode", havingValue = "lazy")
		static class LazyBootstrapMode {

		}

	}

	static class JpaRepositoriesImportSelector implements ImportSelector {

		private static final boolean ENVERS_AVAILABLE = ClassUtils.isPresent(
				"org.springframework.data.envers.repository.config.EnableEnversRepositories",
				JpaRepositoriesImportSelector.class.getClassLoader());

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[] { determineImport() };
		}

		private String determineImport() {
			return ENVERS_AVAILABLE ? EnversRevisionRepositoriesRegistrar.class.getName()
					: DataJpaRepositoriesRegistrar.class.getName();
		}

	}

}
