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

package org.springframework.boot.test.context.assertj;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.error.BasicErrorMessageFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * AssertJ {@link org.assertj.core.api.Assert assertions} that can be applied to a
 * {@link BeanDefinition}.
 *
 * @param <B> the bean definition type
 * @author Dmytro Nosan
 * @since 2.2.0
 * @see ApplicationContextAssert#getBeanDefinition(String)
 */
public class BeanDefinitionAssert<B extends BeanDefinition> extends AbstractObjectAssert<BeanDefinitionAssert<B>, B> {

	BeanDefinitionAssert(B beanDefinition) {
		super(beanDefinition, BeanDefinitionAssert.class);
	}

	/**
	 * Verifies that the bean definition depends on the given bean names.
	 * @param dependsOn the dependent bean names
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition does not depend on the bean names
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getDependsOn()
	 */
	public BeanDefinitionAssert<B> dependsOn(String... dependsOn) {
		BeanDefinition bd = getBeanDefinition();
		for (Object element : ObjectUtils.toObjectArray(dependsOn)) {
			if (!ObjectUtils.containsElement(bd.getDependsOn(), element)) {
				throwAssertionError(new BasicErrorMessageFactory(
						"%nExpecting:%n <%s>%n must depend on:%n <%s>%nbut <%s> is not found",
						ObjectUtils.nullSafeToString(bd.getDependsOn()), ObjectUtils.nullSafeToString(dependsOn),
						element));
			}
		}
		return this;
	}

	/**
	 * Verifies that the bean definition does not depend on the given bean names.
	 * @param dependsOn the dependent bean names
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition at least depends on one of the given
	 * bean names
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getDependsOn()
	 */
	public BeanDefinitionAssert<B> doesNotDependOn(String... dependsOn) {
		BeanDefinition bd = getBeanDefinition();
		for (Object element : ObjectUtils.toObjectArray(dependsOn)) {
			if (ObjectUtils.containsElement(bd.getDependsOn(), element)) {
				throwAssertionError(new BasicErrorMessageFactory(
						"%nExpecting:%n <%s>%n must not depend on:%n <%s>%nbut <%s> is found",
						ObjectUtils.nullSafeToString(bd.getDependsOn()), ObjectUtils.nullSafeToString(dependsOn),
						element));
			}
		}
		return this;
	}

	/**
	 * Verifies that the bean definition has a given bean class name.
	 * @param beanClassName the bean class name of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition does not have a given bean class name
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getBeanClassName()
	 */
	public BeanDefinitionAssert<B> hasBeanClassName(String beanClassName) {
		BeanDefinition bd = getBeanDefinition();
		if (!ObjectUtils.nullSafeEquals(bd.getBeanClassName(), beanClassName)) {
			throwAssertionError(new BasicErrorMessageFactory("%nExpecting:%n Bean class name <%s> must equal to <%s>",
					bd.getBeanClassName(), beanClassName));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition does not have a given bean class name.
	 * @param beanClassName the bean class name of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition has a given bean class name
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getBeanClassName()
	 */
	public BeanDefinitionAssert<B> doesNotHaveBeanClassName(String beanClassName) {
		BeanDefinition bd = getBeanDefinition();
		if (ObjectUtils.nullSafeEquals(bd.getBeanClassName(), beanClassName)) {
			throwAssertionError(
					new BasicErrorMessageFactory("%nExpecting:%n Bean class name <%s> must not equal to <%s>",
							bd.getBeanClassName(), beanClassName));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition has a given bean class.
	 * @param beanClass the bean class of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition does not have a given bean class
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getBeanClassName()
	 */
	public BeanDefinitionAssert<B> hasBeanClass(Class<?> beanClass) {
		return hasBeanClassName(ClassUtils.getQualifiedName(beanClass));
	}

	/**
	 * Verifies that the bean definition does not have a given bean class.
	 * @param beanClass the bean class of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition has a given bean class
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getBeanClassName()
	 */
	public BeanDefinitionAssert<B> doesNotHaveBeanClass(Class<?> beanClass) {
		return doesNotHaveBeanClassName(ClassUtils.getQualifiedName(beanClass));
	}

	/**
	 * Verifies that the bean definition has a given parent name.
	 * @param parentName the parent name of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition does not have a given parent name
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getParentName()
	 */
	public BeanDefinitionAssert<B> hasParentName(String parentName) {
		BeanDefinition bd = getBeanDefinition();
		if (!ObjectUtils.nullSafeEquals(bd.getParentName(), parentName)) {
			throwAssertionError(new BasicErrorMessageFactory("%nExpecting:%n Parent name <%s> must equal to <%s>",
					bd.getParentName(), parentName));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition does not have a given parent name.
	 * @param parentName the parent name of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition has a given parent name
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getParentName()
	 */
	public BeanDefinitionAssert<B> doesNotHaveParentName(String parentName) {
		BeanDefinition bd = getBeanDefinition();
		if (ObjectUtils.nullSafeEquals(bd.getParentName(), parentName)) {
			throwAssertionError(new BasicErrorMessageFactory("%nExpecting:%n Parent name <%s> must not equal to <%s>",
					bd.getParentName(), parentName));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition has a given factory method name.
	 * @param factoryMethodName the factory method name of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition does not have a given factory method
	 * name
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getFactoryMethodName()
	 */
	public BeanDefinitionAssert<B> hasFactoryMethodName(String factoryMethodName) {
		BeanDefinition bd = getBeanDefinition();
		if (!ObjectUtils.nullSafeEquals(bd.getFactoryMethodName(), factoryMethodName)) {
			throwAssertionError(
					new BasicErrorMessageFactory("%nExpecting:%n Factory method name <%s> must equal to <%s>",
							bd.getFactoryMethodName(), factoryMethodName));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition does not have a given factory method name.
	 * @param factoryMethodName the factory method name of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition has a given factory method name
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getFactoryMethodName()
	 */
	public BeanDefinitionAssert<B> doesNotHaveFactoryMethodName(String factoryMethodName) {
		BeanDefinition bd = getBeanDefinition();
		if (ObjectUtils.nullSafeEquals(bd.getFactoryMethodName(), factoryMethodName)) {
			throwAssertionError(
					new BasicErrorMessageFactory("%nExpecting:%n Factory method name <%s> must not equal to <%s>",
							bd.getFactoryMethodName(), factoryMethodName));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition has a given factory bean name.
	 * @param factoryBeanName the factory bean name of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition does not have a given factory bean
	 * name
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getFactoryBeanName()
	 */
	public BeanDefinitionAssert<B> hasFactoryBeanName(String factoryBeanName) {
		BeanDefinition bd = getBeanDefinition();
		if (!ObjectUtils.nullSafeEquals(bd.getFactoryBeanName(), factoryBeanName)) {
			throwAssertionError(new BasicErrorMessageFactory("%nExpecting:%n Factory bean name <%s> must equal to <%s>",
					bd.getFactoryBeanName(), factoryBeanName));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition does not have a given factory bean name.
	 * @param factoryBeanName the factory bean name of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition has a given factory bean name
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getFactoryBeanName()
	 */
	public BeanDefinitionAssert<B> doesNotHaveFactoryBeanName(String factoryBeanName) {
		BeanDefinition bd = getBeanDefinition();
		if (ObjectUtils.nullSafeEquals(bd.getFactoryBeanName(), factoryBeanName)) {
			throwAssertionError(
					new BasicErrorMessageFactory("%nExpecting:%n Factory bean name <%s> must not equal to <%s>",
							bd.getFactoryBeanName(), factoryBeanName));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition has a given init method name.
	 * @param initMethodName the init method name of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition does not have a given init method
	 * name
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getInitMethodName()
	 */
	public BeanDefinitionAssert<B> hasInitMethodName(String initMethodName) {
		BeanDefinition bd = getBeanDefinition();
		if (!ObjectUtils.nullSafeEquals(bd.getInitMethodName(), initMethodName)) {
			throwAssertionError(new BasicErrorMessageFactory("%nExpecting:%n Init method name <%s> must equal to <%s>",
					bd.getInitMethodName(), initMethodName));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition does not have an init method name.
	 * @param initMethodName the init method name of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition has a given init method name
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getInitMethodName()
	 */
	public BeanDefinitionAssert<B> doesNotHaveInitMethodName(String initMethodName) {
		BeanDefinition bd = getBeanDefinition();
		if (ObjectUtils.nullSafeEquals(bd.getInitMethodName(), initMethodName)) {
			throwAssertionError(
					new BasicErrorMessageFactory("%nExpecting:%n Init method name <%s> must not equal to <%s>",
							bd.getInitMethodName(), initMethodName));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition has a given destroy method name.
	 * @param destroyMethodName the destroy method name of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition does not have a given destroy method
	 * name
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getDestroyMethodName()
	 */
	public BeanDefinitionAssert<B> hasDestroyMethodName(String destroyMethodName) {
		BeanDefinition bd = getBeanDefinition();
		if (!ObjectUtils.nullSafeEquals(bd.getDestroyMethodName(), destroyMethodName)) {
			throwAssertionError(
					new BasicErrorMessageFactory("%nExpecting:%n Destroy method name <%s> must equal to <%s>",
							bd.getDestroyMethodName(), destroyMethodName));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition does not have a destroy method name.
	 * @param destroyMethodName the destroy method name of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition has a given destroy method name
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getDestroyMethodName()
	 */
	public BeanDefinitionAssert<B> doesNotHaveDestroyMethodName(String destroyMethodName) {
		BeanDefinition bd = getBeanDefinition();
		if (ObjectUtils.nullSafeEquals(bd.getDestroyMethodName(), destroyMethodName)) {
			throwAssertionError(
					new BasicErrorMessageFactory("%nExpecting:%n Destroy method name <%s> must not equal to <%s>",
							bd.getDestroyMethodName(), destroyMethodName));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition has a given scope.
	 * @param scope the scope of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition does not have a given a scope
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getScope()
	 */
	public BeanDefinitionAssert<B> hasScope(String scope) {
		BeanDefinition bd = getBeanDefinition();
		if (!ObjectUtils.nullSafeEquals(bd.getScope(), scope)) {
			throwAssertionError(
					new BasicErrorMessageFactory("%nExpecting:%n Scope <%s> must equal to <%s>", bd.getScope(), scope));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition does not have a given scope.
	 * @param scope the scope of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition has a given a scope
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getScope()
	 */
	public BeanDefinitionAssert<B> doesNotHaveScope(String scope) {
		BeanDefinition bd = getBeanDefinition();
		if (ObjectUtils.nullSafeEquals(bd.getScope(), scope)) {
			throwAssertionError(new BasicErrorMessageFactory("%nExpecting:%n Scope <%s> must not equal to <%s>",
					bd.getScope(), scope));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition has a given role.
	 * @param role the role of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition does not have a given a role
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#getRole()
	 */
	public BeanDefinitionAssert<B> hasRole(int role) {
		BeanDefinition bd = getBeanDefinition();
		if (bd.getRole() != role) {
			throwAssertionError(
					new BasicErrorMessageFactory("%nExpecting:%n Role <%s> must equal to <%s>", bd.getRole(), role));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition does not have a given role.
	 * @param role the role of the bean definition.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition has a given a role
	 * @see BeanDefinition#getRole()
	 */
	public BeanDefinitionAssert<B> doesNotHaveRole(int role) {
		BeanDefinition bd = getBeanDefinition();
		if (bd.getRole() == role) {
			throwAssertionError(new BasicErrorMessageFactory("%nExpecting:%n Role <%s> must not equal to <%s>",
					bd.getRole(), role));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition is a singleton.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition is not a singleton
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#isSingleton()
	 */
	public BeanDefinitionAssert<B> isSingleton() {
		BeanDefinition bd = getBeanDefinition();
		if (!bd.isSingleton()) {
			throwAssertionError(new BasicErrorMessageFactory(
					"%nExpecting:%n must be a singleton%nbut <%s> scope is found", bd.getScope()));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition is not a singleton.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition is a singleton
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#isSingleton()
	 */
	public BeanDefinitionAssert<B> isNotSingleton() {
		BeanDefinition bd = getBeanDefinition();
		if (bd.isSingleton()) {
			throwAssertionError(new BasicErrorMessageFactory(
					"%nExpecting:%n must not be a singleton%nbut <%s> scope is found", bd.getScope()));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition is a prototype.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition is not a prototype
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#isPrototype()
	 */
	public BeanDefinitionAssert<B> isPrototype() {
		BeanDefinition bd = getBeanDefinition();
		if (!bd.isPrototype()) {
			throwAssertionError(new BasicErrorMessageFactory(
					"%nExpecting:%n must be a prototype%nbut <%s> scope is found", bd.getScope()));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition is not a prototype.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition is a prototype
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#isPrototype()
	 */
	public BeanDefinitionAssert<B> isNotPrototype() {
		BeanDefinition bd = getBeanDefinition();
		if (bd.isPrototype()) {
			throwAssertionError(new BasicErrorMessageFactory(
					"%nExpecting:%n must not be a prototype%nbut <%s> scope is found", bd.getScope()));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition is an abstract.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition is not an abstract
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#isAbstract()
	 */
	public BeanDefinitionAssert<B> isAbstract() {
		BeanDefinition bd = getBeanDefinition();
		if (!bd.isAbstract()) {
			throwAssertionError(new BasicErrorMessageFactory("%nExpecting:%n must be an abstract"));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition is not an abstract.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition is an abstract
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#isAbstract()
	 */
	public BeanDefinitionAssert<B> isNotAbstract() {
		BeanDefinition bd = getBeanDefinition();
		if (bd.isAbstract()) {
			throwAssertionError(new BasicErrorMessageFactory("%nExpecting:%n must not be an abstract"));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition is a lazy.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition is not a lazy
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#isLazyInit()
	 */
	public BeanDefinitionAssert<B> isLazy() {
		BeanDefinition bd = getBeanDefinition();
		if (!bd.isLazyInit()) {
			throwAssertionError(new BasicErrorMessageFactory("%nExpecting:%n must be a lazy"));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition is not a lazy.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition is a lazy
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#isLazyInit()
	 */
	public BeanDefinitionAssert<B> isNotLazy() {
		BeanDefinition bd = getBeanDefinition();
		if (bd.isLazyInit()) {
			throwAssertionError(new BasicErrorMessageFactory("%nExpecting:%n must not be a lazy"));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition is a primary.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition is not a primary
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#isPrimary()
	 */
	public BeanDefinitionAssert<B> isPrimary() {
		BeanDefinition bd = getBeanDefinition();
		if (!bd.isPrimary()) {
			throwAssertionError(new BasicErrorMessageFactory("%nExpecting:%n must be a primary"));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition is not a primary.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition is a primary
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#isPrimary()
	 */
	public BeanDefinitionAssert<B> isNotPrimary() {
		BeanDefinition bd = getBeanDefinition();
		if (bd.isPrimary()) {
			throwAssertionError(new BasicErrorMessageFactory("%nExpecting:%n must not be a primary"));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition is a candidate for getting autowired into some
	 * other bean.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition is not a candidate for getting
	 * autowired
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#isAutowireCandidate()
	 */
	public BeanDefinitionAssert<B> isAutowireCandidate() {
		BeanDefinition bd = getBeanDefinition();
		if (!bd.isAutowireCandidate()) {
			throwAssertionError(new BasicErrorMessageFactory(
					"%nExpecting:%n must be a candidate for getting autowired into some other bean"));
		}
		return this;
	}

	/**
	 * Verifies that the bean definition is not a candidate for getting autowired into
	 * some other bean.
	 * @return {@code this} assertion object.
	 * @throws AssertionError if the bean definition is a candidate for getting autowired
	 * @throws AssertionError if the bean definition value is {@code null}
	 * @see BeanDefinition#isAutowireCandidate()
	 */
	public BeanDefinitionAssert<B> isNotAutowireCandidate() {
		BeanDefinition bd = getBeanDefinition();
		if (bd.isAutowireCandidate()) {
			throwAssertionError(new BasicErrorMessageFactory(
					"%nExpecting:%n must not be a candidate for getting autowired into some other bean"));
		}
		return this;
	}

	protected final BeanDefinition getBeanDefinition() {
		B bd = this.actual;
		if (bd == null) {
			throwAssertionError(new BasicErrorMessageFactory("%nExpecting: Actual must not to be null"));
		}
		return bd;
	}

}
