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

package org.springframework.boot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.Ordered;

/**
 * {@link BeanFactoryPostProcessor} to set the lazy attribute on bean definition.
 *
 * <P>
 * This processor will not touch a bean definition that has already had its "lazy" flag
 * explicitly set to "false".
 *
 * <P>
 * There are edge cases in which it is not easy to explicitly set the "lazy" flag to
 * "false" (such as in DSLs that dynamically create additional beans) and therefore this
 * class uses a customizer strategy that allows downstream projects to contribute
 * predicates which impact if a class is considered for lazy-loading.
 *
 * <P>
 * Because this is a BeanFactoryPostProcessor, this class does not use dependency
 * injection to collect the customizers. The post processor actually makes two passes
 * through the bean definitions; the first is used to find and instantiate any
 * {@link org.springframework.boot.EagerLoadingBeanDefinitionPredicate} and the second
 * pass is where bean definitions are marked as lazy.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Tyler Van Gorder
 * @since 2.2.0
 */
public final class LazyInitializationBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		List<EagerLoadingBeanDefinitionPredicate> eagerPredicateList = getEagerLoadingPredicatesFromContext(
				beanFactory);

		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			if (eagerPredicateList.stream()
					.anyMatch((predicate) -> predicate.test(beanFactory.getType(beanName, false)))) {
				continue;
			}
			if (beanDefinition instanceof AbstractBeanDefinition) {
				Boolean lazyInit = ((AbstractBeanDefinition) beanDefinition).getLazyInit();
				if (lazyInit != null && !lazyInit) {
					continue;
				}
			}
			beanDefinition.setLazyInit(true);
		}
	}

	/**
	 * This method extracts the list of
	 * {@link org.springframework.boot.EagerLoadingBeanDefinitionPredicate} beans from the
	 * bean factory. Because this method is called early in the factory life cycle, we
	 * take care not to force the eager initialization of factory beans.
	 * @param beanFactory bean factory passed into the post-processor.
	 * @return a list of {@link EagerLoadingBeanDefinitionPredicate} that can be used to
	 * customize the behavior of this processor.
	 */
	private List<EagerLoadingBeanDefinitionPredicate> getEagerLoadingPredicatesFromContext(
			ConfigurableListableBeanFactory beanFactory) {

		Map<String, EagerLoadingBeanDefinitionPredicate> eagerPredicates = beanFactory
				.getBeansOfType(EagerLoadingBeanDefinitionPredicate.class, false, false);

		return new ArrayList<>(eagerPredicates.values());

	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
