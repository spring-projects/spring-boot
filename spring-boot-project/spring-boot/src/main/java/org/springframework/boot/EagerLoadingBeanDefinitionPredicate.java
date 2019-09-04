/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.function.Predicate;

/**
 * This predicate can be implemented by downstream projects to customize the behavior of
 * the {@link LazyInitializationBeanFactoryPostProcessor}.
 *
 * <P>
 * There are edge cases (such as in DSLs that dynamically create additional beans) in
 * which it is not easy to explicitly exclude a class from the lazy-loading behavior.
 * Adding an instance of this predicate to the application context can be used for these
 * edge cases.
 * <P>
 * Returning "true" from this predicate will exclude a class from the lazy-loading
 * process.
 *
 * <P>
 * Example:
 * <P>
 * <pre>
 * {@code
 *
 * &#64;Bean
 * public static EagerLoadingBeanDefinitionPredicate eagerLoadingBeanDefinitionPredicate() {
 *   return IntegrationFlow.class::isAssignableFrom;
 * }}</pre>
 *
 * WARNING: Beans of this type will be instantiated very early in the spring application
 * life cycle.
 *
 * @author Tyler Van Gorder
 * @since 2.2.0
 */
public interface EagerLoadingBeanDefinitionPredicate extends Predicate<Class<?>> {

}
