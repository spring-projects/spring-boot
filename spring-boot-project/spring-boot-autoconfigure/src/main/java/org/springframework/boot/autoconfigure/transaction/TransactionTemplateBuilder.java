/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.transaction;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Builder that can be used to configure and create a {@link TransactionTemplate}.
 *
 * In addition to {@code int} for propagation and isolation levels, enum of
 * {@link Propagation} and {@link Isolation} are supported.
 *
 * @author Tadaya Tsuyukubo
 */
public class TransactionTemplateBuilder {

	private Set<TransactionTemplateCustomizer> transactionTemplateCustomizers;

	private final PlatformTransactionManager transactionManager;

	private final Integer propagationBehavior;

	private final String propagationBehaviorName;

	private final Integer isolationLevel;

	private final String isolationLevelName;

	private final Duration timeout;

	private final Boolean readOnly;

	private final String name;

	/**
	 * Create a new {@link TransactionTemplateBuilder} instance.
	 * @param customizers any {@link TransactionTemplateCustomizer
	 * TransactionTemplateCustomizers} that should be applied when the
	 * {@link TransactionTemplate} is built
	 */
	public TransactionTemplateBuilder(TransactionTemplateCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.transactionTemplateCustomizers = Collections
				.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(customizers)));
		this.transactionManager = null;
		this.propagationBehavior = null;
		this.propagationBehaviorName = null;
		this.isolationLevel = null;
		this.isolationLevelName = null;
		this.timeout = null;
		this.readOnly = null;
		this.name = null;
	}

	public TransactionTemplateBuilder(
			Set<TransactionTemplateCustomizer> transactionTemplateCustomizers,
			PlatformTransactionManager transactionManager, Integer propagationBehavior,
			String propagationBehaviorName, Integer isolationLevel,
			String isolationLevelName, Duration timeout, Boolean readOnly, String name) {
		this.transactionTemplateCustomizers = transactionTemplateCustomizers;
		this.transactionManager = transactionManager;
		this.propagationBehavior = propagationBehavior;
		this.propagationBehaviorName = propagationBehaviorName;
		this.isolationLevel = isolationLevel;
		this.isolationLevelName = isolationLevelName;
		this.timeout = timeout;
		this.readOnly = readOnly;
		this.name = name;
	}

	/**
	 * Set the {@link TransactionTemplateBuilder TransactionTemplateBuilders} that should
	 * be applied to the {@link TransactionTemplate}. Customizers are applied in the order
	 * that they were added after builder configuration has been applied. Setting this
	 * value will replace any previously configured customizers.
	 * @param customizers the customizers to set
	 * @return a new builder instance
	 * @see #additionalCustomizers(TransactionTemplateCustomizer...)
	 */
	public TransactionTemplateBuilder customizers(
			TransactionTemplateCustomizer... customizers) {
		Assert.notNull(customizers, "TransactionTemplateCustomizers must not be null");
		return customizers(Arrays.asList(customizers));
	}

	/**
	 * Set the {@link TransactionTemplateBuilder TransactionTemplateBuilders} that should
	 * be applied to the {@link TransactionTemplate}. Customizers are applied in the order
	 * that they were added after builder configuration has been applied. Setting this
	 * value will replace any previously configured customizers.
	 * @param customizers the customizers to set
	 * @return a new builder instance
	 * @see #additionalCustomizers(TransactionTemplateCustomizer...)
	 */
	public TransactionTemplateBuilder customizers(
			Collection<? extends TransactionTemplateCustomizer> customizers) {
		Assert.notNull(customizers, "TransactionTemplateCustomizers must not be null");
		return new TransactionTemplateBuilder(
				Collections.unmodifiableSet(new LinkedHashSet<>(customizers)),
				this.transactionManager, this.propagationBehavior,
				this.propagationBehaviorName, this.isolationLevel,
				this.isolationLevelName, this.timeout, this.readOnly, this.name);
	}

	/**
	 * Add {@link TransactionTemplateBuilder TransactionTemplateBuilders} that should be
	 * applied to the {@link TransactionTemplate}. Customizers are applied in the order
	 * that they were added after builder configuration has been applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 * @see #customizers(TransactionTemplateCustomizer...)
	 */
	public TransactionTemplateBuilder additionalCustomizers(
			TransactionTemplateCustomizer... customizers) {
		Assert.notNull(customizers, "TransactionTemplateCustomizers must not be null");
		return additionalCustomizers(Arrays.asList(customizers));
	}

	/**
	 * Add {@link TransactionTemplateBuilder TransactionTemplateBuilders} that should be
	 * applied to the {@link TransactionTemplate}. Customizers are applied in the order
	 * that they were added after builder configuration has been applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 * @see #customizers(TransactionTemplateCustomizer...)
	 */
	public TransactionTemplateBuilder additionalCustomizers(
			Collection<? extends TransactionTemplateCustomizer> customizers) {
		Assert.notNull(customizers, "TransactionTemplateCustomizers must not be null");
		return new TransactionTemplateBuilder(
				append(this.transactionTemplateCustomizers, customizers),
				this.transactionManager, this.propagationBehavior,
				this.propagationBehaviorName, this.isolationLevel,
				this.isolationLevelName, this.timeout, this.readOnly, this.name);
	}

	public TransactionTemplateBuilder transactionManager(
			PlatformTransactionManager transactionManager) {
		return new TransactionTemplateBuilder(this.transactionTemplateCustomizers,
				transactionManager, this.propagationBehavior,
				this.propagationBehaviorName, this.isolationLevel,
				this.isolationLevelName, this.timeout, this.readOnly, this.name);
	}

	public TransactionTemplateBuilder propagationBehavior(int propagationBehavior) {
		return new TransactionTemplateBuilder(this.transactionTemplateCustomizers,
				this.transactionManager, propagationBehavior,
				this.propagationBehaviorName, this.isolationLevel,
				this.isolationLevelName, this.timeout, this.readOnly, this.name);
	}

	public TransactionTemplateBuilder propagationBehaviorName(
			String propagationBehaviorName) {
		return new TransactionTemplateBuilder(this.transactionTemplateCustomizers,
				this.transactionManager, this.propagationBehavior,
				propagationBehaviorName, this.isolationLevel, this.isolationLevelName,
				this.timeout, this.readOnly, this.name);
	}

	public TransactionTemplateBuilder propagation(Propagation propagation) {
		return new TransactionTemplateBuilder(this.transactionTemplateCustomizers,
				this.transactionManager,
				(propagation != null) ? propagation.value() : null,
				this.propagationBehaviorName, this.isolationLevel,
				this.isolationLevelName, this.timeout, this.readOnly, this.name);
	}

	public TransactionTemplateBuilder isolationLevel(int isolationLevel) {
		return new TransactionTemplateBuilder(this.transactionTemplateCustomizers,
				this.transactionManager, this.propagationBehavior,
				this.propagationBehaviorName, isolationLevel, this.isolationLevelName,
				this.timeout, this.readOnly, this.name);
	}

	public TransactionTemplateBuilder isolationLevelName(String isolationLevelName) {
		return new TransactionTemplateBuilder(this.transactionTemplateCustomizers,
				this.transactionManager, this.propagationBehavior,
				this.propagationBehaviorName, this.isolationLevel, isolationLevelName,
				this.timeout, this.readOnly, this.name);
	}

	public TransactionTemplateBuilder isolation(Isolation isolation) {
		return new TransactionTemplateBuilder(this.transactionTemplateCustomizers,
				this.transactionManager, this.propagationBehavior,
				this.propagationBehaviorName,
				(isolation != null) ? isolation.value() : null, this.isolationLevelName,
				this.timeout, this.readOnly, this.name);
	}

	public TransactionTemplateBuilder timeout(int timeoutSecond) {
		return new TransactionTemplateBuilder(this.transactionTemplateCustomizers,
				this.transactionManager, this.propagationBehavior,
				this.propagationBehaviorName, this.isolationLevel,
				this.isolationLevelName, Duration.ofSeconds(timeoutSecond), this.readOnly,
				this.name);
	}

	public TransactionTemplateBuilder timeout(Duration timeout) {
		return new TransactionTemplateBuilder(this.transactionTemplateCustomizers,
				this.transactionManager, this.propagationBehavior,
				this.propagationBehaviorName, this.isolationLevel,
				this.isolationLevelName, timeout, this.readOnly, this.name);
	}

	public TransactionTemplateBuilder readOnly(boolean readOnly) {
		return new TransactionTemplateBuilder(this.transactionTemplateCustomizers,
				this.transactionManager, this.propagationBehavior,
				this.propagationBehaviorName, this.isolationLevel,
				this.isolationLevelName, this.timeout, readOnly, this.name);
	}

	public TransactionTemplateBuilder name(String name) {
		return new TransactionTemplateBuilder(this.transactionTemplateCustomizers,
				this.transactionManager, this.propagationBehavior,
				this.propagationBehaviorName, this.isolationLevel,
				this.isolationLevelName, this.timeout, this.readOnly, name);
	}

	public TransactionTemplate build() {
		return build(TransactionTemplate.class);
	}

	public <T extends TransactionTemplate> T build(Class<T> transactionTemplateClass) {
		return configure(BeanUtils.instantiateClass(transactionTemplateClass));
	}

	public <T extends TransactionTemplate> T configure(T transactionTemplate) {
		if (this.transactionManager != null) {
			transactionTemplate.setTransactionManager(this.transactionManager);
		}

		if (this.propagationBehavior != null) {
			transactionTemplate.setPropagationBehavior(this.propagationBehavior);
		}
		if (this.propagationBehaviorName != null) {
			transactionTemplate.setPropagationBehaviorName(this.propagationBehaviorName);
		}

		if (this.isolationLevel != null) {
			transactionTemplate.setIsolationLevel(this.isolationLevel);
		}
		if (this.isolationLevelName != null) {
			transactionTemplate.setIsolationLevelName(this.isolationLevelName);
		}

		if (this.timeout != null) {
			transactionTemplate.setTimeout((int) this.timeout.getSeconds());
		}
		if (this.readOnly != null) {
			transactionTemplate.setReadOnly(this.readOnly);
		}
		if (this.name != null) {
			transactionTemplate.setName(this.name);
		}

		if (!CollectionUtils.isEmpty(this.transactionTemplateCustomizers)) {
			for (TransactionTemplateCustomizer customizer : this.transactionTemplateCustomizers) {
				customizer.customize(transactionTemplate);
			}
		}

		return transactionTemplate;
	}

	private <T> Set<T> append(Set<T> set, Collection<? extends T> additions) {
		Set<T> result = new LinkedHashSet<>((set != null) ? set : Collections.emptySet());
		result.addAll(additions);
		return Collections.unmodifiableSet(result);
	}

}
