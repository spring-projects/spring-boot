/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.lettuce.core.dynamic.support.ResolvableType;
import org.junit.jupiter.api.Test;

import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AbstractCompositeHealthContributorConfiguration}.
 *
 * @param <C> the contributor type
 * @param <I> the health indicator type
 * @author Phillip Webb
 */
abstract class AbstractCompositeHealthContributorConfigurationTests<C, I extends C> {

	private final Class<?> indicatorType;

	AbstractCompositeHealthContributorConfigurationTests() {
		ResolvableType type = ResolvableType.forClass(AbstractCompositeHealthContributorConfigurationTests.class,
				getClass());
		this.indicatorType = type.resolveGeneric(1);
	}

	@Test
	void createContributorWhenBeansIsEmptyThrowsException() {
		Map<String, TestBean> beans = Collections.emptyMap();
		assertThatIllegalArgumentException().isThrownBy(() -> newComposite().createContributor(beans))
			.withMessage("Beans must not be empty");
	}

	@Test
	void createContributorWhenBeansHasSingleElementCreatesIndicator() {
		Map<String, TestBean> beans = Collections.singletonMap("test", new TestBean());
		C contributor = newComposite().createContributor(beans);
		assertThat(contributor).isInstanceOf(this.indicatorType);
	}

	@Test
	void createContributorWhenBeansHasMultipleElementsCreatesComposite() {
		Map<String, TestBean> beans = new LinkedHashMap<>();
		beans.put("test1", new TestBean());
		beans.put("test2", new TestBean());
		C contributor = newComposite().createContributor(beans);
		assertThat(contributor).isNotInstanceOf(this.indicatorType);
		assertThat(ClassUtils.getShortName(contributor.getClass())).startsWith("Composite");
	}

	protected abstract AbstractCompositeHealthContributorConfiguration<C, I, TestBean> newComposite();

	static class TestBean {

	}

}
