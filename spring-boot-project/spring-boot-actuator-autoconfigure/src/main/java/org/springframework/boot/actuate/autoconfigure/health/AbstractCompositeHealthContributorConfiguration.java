/*
 * Copyright 2012-2019 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * Base class for health contributor configurations that can combine source beans into a
 * composite.
 *
 * @param <C> the contributor type
 * @param <I> the health indicator type
 * @param <B> the bean type
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.2.0
 */
public abstract class AbstractCompositeHealthContributorConfiguration<C, I extends C, B> {

	private final Class<?> indicatorType;

	private final Class<?> beanType;

	AbstractCompositeHealthContributorConfiguration() {
		ResolvableType type = ResolvableType.forClass(AbstractCompositeHealthContributorConfiguration.class,
				getClass());
		this.indicatorType = type.resolveGeneric(1);
		this.beanType = type.resolveGeneric(2);

	}

	protected final C createContributor(Map<String, B> beans) {
		Assert.notEmpty(beans, "Beans must not be empty");
		if (beans.size() == 1) {
			return createIndicator(beans.values().iterator().next());
		}
		return createComposite(beans);
	}

	protected abstract C createComposite(Map<String, B> beans);

	@SuppressWarnings("unchecked")
	protected I createIndicator(B bean) {
		try {
			Constructor<I> constructor = (Constructor<I>) this.indicatorType.getDeclaredConstructor(this.beanType);
			return BeanUtils.instantiateClass(constructor, bean);
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Unable to create health indicator " + this.indicatorType + " for bean type " + this.beanType, ex);
		}
	}

}
