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
import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.mockito.InOrder;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link TransactionTemplateBuilder}.
 *
 * @author Tadaya Tsuyukubo
 */
public class TransactionTemplateBuilderTests {

	private TransactionTemplateBuilder builder = new TransactionTemplateBuilder();

	@Test
	public void createWhenCustomizersAreNullShouldThrowException() {
		TransactionTemplateCustomizer[] customizers = null;
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new TransactionTemplateBuilder(customizers))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	public void createWithCustomizersShouldApplyCustomizers() {
		TransactionTemplateCustomizer customizer = mock(
				TransactionTemplateCustomizer.class);
		TransactionTemplate template = new TransactionTemplateBuilder(customizer).build();
		verify(customizer).customize(template);
	}

	@Test
	public void customizersWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.builder.customizers((TransactionTemplateCustomizer[]) null))
				.withMessageContaining("TransactionTemplateCustomizers must not be null");
	}

	@Test
	public void customizersCollectionWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.builder.customizers((Set<TransactionTemplateCustomizer>) null))
				.withMessageContaining("TransactionTemplateCustomizers must not be null");
	}

	@Test
	public void customizersShouldApply() {
		TransactionTemplateCustomizer customizer = mock(
				TransactionTemplateCustomizer.class);
		TransactionTemplate template = this.builder.customizers(customizer).build();
		verify(customizer).customize(template);
	}

	@Test
	public void customizersShouldReplaceExisting() {
		TransactionTemplateCustomizer customizer1 = mock(
				TransactionTemplateCustomizer.class);
		TransactionTemplateCustomizer customizer2 = mock(
				TransactionTemplateCustomizer.class);
		TransactionTemplate template = this.builder.customizers(customizer1)
				.customizers(Collections.singleton(customizer2)).build();
		verifyZeroInteractions(customizer1);
		verify(customizer2).customize(template);
	}

	@Test
	public void additionalCustomizersWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder
						.additionalCustomizers((TransactionTemplateCustomizer[]) null))
				.withMessageContaining("TransactionTemplateCustomizers must not be null");
	}

	@Test
	public void additionalCustomizersCollectionWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder
						.additionalCustomizers((Set<TransactionTemplateCustomizer>) null))
				.withMessageContaining("TransactionTemplateCustomizers must not be null");
	}

	@Test
	public void additionalCustomizersShouldAddToExisting() {
		TransactionTemplateCustomizer customizer1 = mock(
				TransactionTemplateCustomizer.class);
		TransactionTemplateCustomizer customizer2 = mock(
				TransactionTemplateCustomizer.class);
		TransactionTemplate template = this.builder.customizers(customizer1)
				.additionalCustomizers(customizer2).build();
		InOrder inOrder = inOrder(customizer1, customizer2);
		inOrder.verify(customizer1).customize(template);
		inOrder.verify(customizer2).customize(template);
	}

	@Test
	public void buildShouldReturnTransactionTemplate() {
		TransactionTemplate template = this.builder.build();
		assertThat(template.getClass()).isEqualTo(TransactionTemplate.class);
	}

	@Test
	public void buildClassShouldReturnClassInstance() {
		TransactionTemplateSubclass template = this.builder
				.build(TransactionTemplateSubclass.class);
		assertThat(template.getClass()).isEqualTo(TransactionTemplateSubclass.class);
	}

	@Test
	public void propagationCanBeNullToUseDefault() {
		TransactionTemplate template = this.builder.propagation(null).build();
		assertThat(template.getPropagationBehavior())
				.isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
	}

	@Test
	public void propagationBehaviorNameCanBeNullToUseDefault() {
		TransactionTemplate template = this.builder.propagationBehaviorName(null).build();
		assertThat(template.getPropagationBehavior())
				.isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
	}

	@Test
	public void isolationCanBeNullToUseDefault() {
		TransactionTemplate template = this.builder.isolation(null).build();
		assertThat(template.getIsolationLevel())
				.isEqualTo(TransactionDefinition.ISOLATION_DEFAULT);
	}

	@Test
	public void isolationLevelNameCanBeNullToUseDefault() {
		TransactionTemplate template = this.builder.isolationLevelName(null).build();
		assertThat(template.getIsolationLevel())
				.isEqualTo(TransactionDefinition.ISOLATION_DEFAULT);
	}

	@Test
	public void timeoutCanBeNullToUseDefault() {
		TransactionTemplate template = this.builder.timeout(null).build();
		assertThat(template.getTimeout())
				.isEqualTo(TransactionDefinition.TIMEOUT_DEFAULT);
	}

	@Test
	public void defaultValues() {
		TransactionTemplate template = this.builder.build();
		assertThat(template.getPropagationBehavior())
				.isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
		assertThat(template.getIsolationLevel())
				.isEqualTo(TransactionDefinition.ISOLATION_DEFAULT);
		assertThat(template.getTimeout())
				.isEqualTo(TransactionDefinition.TIMEOUT_DEFAULT);
		assertThat(template.isReadOnly()).isFalse();
		assertThat(template.getName()).isNull();
		assertThat(template.getTransactionManager()).isNull();
	}

	@Test
	public void setValues() {
		PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);

		TransactionTemplate template = this.builder.propagation(Propagation.MANDATORY)
				.isolation(Isolation.READ_COMMITTED).timeout(Duration.ofSeconds(99))
				.readOnly(true).name("foo").transactionManager(txManager).build();

		assertThat(template.getPropagationBehavior())
				.isEqualTo(TransactionDefinition.PROPAGATION_MANDATORY);
		assertThat(template.getIsolationLevel())
				.isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED);
		assertThat(template.getTimeout()).isEqualTo(99);
		assertThat(template.isReadOnly()).isTrue();
		assertThat(template.getName()).isEqualTo("foo");
		assertThat(template.getTransactionManager()).isSameAs(txManager);
	}

	static class TransactionTemplateSubclass extends TransactionTemplate {

	}

}
