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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link BeanDefinitionAssert}.
 *
 * @author Dmytro Nosan
 */
class BeanDefinitionAssertTests {

	private final GenericBeanDefinition beanDefinition = new GenericBeanDefinition();

	@Test
	void dependsOnShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertBeanDefinition().dependsOn("foo"))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must depend on:")
				.withMessageContaining("foo").withMessageContaining("is not found");
	}

	@Test
	void dependsOnShouldPass() {
		this.beanDefinition.setDependsOn("foo", "bar");
		assertBeanDefinition().dependsOn("foo").dependsOn("bar").dependsOn("foo", "bar");
	}

	@Test
	void doesNotDependOnShouldPass() {
		assertBeanDefinition().doesNotDependOn("foo", "bar");
	}

	@Test
	void doesNotDependOnShouldFail() {
		this.beanDefinition.setDependsOn("foo", "bar");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertBeanDefinition().doesNotDependOn("foo"))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must not depend on:")
				.withMessageContaining("foo").withMessageContaining("is found");
	}

	@Test
	void hasBeanClassNameShouldFail() {
		this.beanDefinition.setBeanClassName(Object.class.getTypeName());
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().hasBeanClassName(String.class.getTypeName()))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must equal to")
				.withMessageContaining(Object.class.getTypeName()).withMessageContaining(String.class.getTypeName());
	}

	@Test
	void hasBeanClassNameShouldPass() {
		this.beanDefinition.setBeanClassName(Object.class.getTypeName());
		assertBeanDefinition().hasBeanClassName(Object.class.getTypeName());
	}

	@Test
	void hasBeanClassShouldFail() {
		this.beanDefinition.setBeanClass(Object.class);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().hasBeanClass(String.class))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must equal to")
				.withMessageContaining(Object.class.getTypeName()).withMessageContaining(String.class.getTypeName());
	}

	@Test
	void hasBeanClassShouldPass() {
		this.beanDefinition.setBeanClass(Object.class);
		assertBeanDefinition().hasBeanClass(Object.class);
	}

	@Test
	void doesNotHaveBeanClassNameShouldPass() {
		this.beanDefinition.setBeanClassName(Object.class.getTypeName());
		assertBeanDefinition().doesNotHaveBeanClassName(String.class.getTypeName());
	}

	@Test
	void doesNotHaveBeanClassNameShouldFail() {
		this.beanDefinition.setBeanClassName(Object.class.getTypeName());
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().doesNotHaveBeanClassName(Object.class.getTypeName()))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must not equal to")
				.withMessageContaining(Object.class.getTypeName());
	}

	@Test
	void doesNotHaveBeanClassShouldPass() {
		this.beanDefinition.setBeanClass(Object.class);
		assertBeanDefinition().doesNotHaveBeanClass(String.class);
	}

	@Test
	void doesNotHaveBeanClassShouldFail() {
		this.beanDefinition.setBeanClass(Object.class);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().doesNotHaveBeanClass(Object.class))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must not equal to")
				.withMessageContaining(Object.class.getTypeName());
	}

	@Test
	void hasParentNameShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertBeanDefinition().hasParentName("bar"))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must equal to")
				.withMessageContaining("bar");
	}

	@Test
	void hasParentNameShouldPass() {
		this.beanDefinition.setParentName("bar");
		assertBeanDefinition().hasParentName("bar");
	}

	@Test
	void doesNotHaveParentNameShouldFail() {
		this.beanDefinition.setParentName("bar");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().doesNotHaveParentName("bar"))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must not equal to")
				.withMessageContaining("bar");
	}

	@Test
	void doesNotHaveParentNameShouldPass() {
		assertBeanDefinition().doesNotHaveParentName("bar");
	}

	@Test
	void hasFactoryMethodNameShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().hasFactoryMethodName("createFoo"))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must equal to")
				.withMessageContaining("createFoo");
	}

	@Test
	void hasFactoryMethodNameShouldPass() {
		this.beanDefinition.setFactoryMethodName("createFoo");
		assertBeanDefinition().hasFactoryMethodName("createFoo");
	}

	@Test
	void doesNotHaveFactoryMethodNameShouldPass() {
		assertBeanDefinition().doesNotHaveFactoryMethodName("createFoo");
	}

	@Test
	void doesNotHaveFactoryMethodNameShouldFail() {
		this.beanDefinition.setFactoryMethodName("createFoo");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().doesNotHaveFactoryMethodName("createFoo"))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must not equal to")
				.withMessageContaining("createFoo");
	}

	@Test
	void hasFactoryBeanNameShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().hasFactoryBeanName("fooFactoryBean"))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must equal to")
				.withMessageContaining("fooFactoryBean");
	}

	@Test
	void hasFactoryBeanNameShouldPass() {
		this.beanDefinition.setFactoryBeanName("fooFactoryBean");
		assertBeanDefinition().hasFactoryBeanName("fooFactoryBean");
	}

	@Test
	void doesNotHaveFactoryBeanNameShouldFail() {
		this.beanDefinition.setFactoryBeanName("fooFactoryBean");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().doesNotHaveFactoryBeanName("fooFactoryBean"))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must not equal to")
				.withMessageContaining("fooFactoryBean");
	}

	@Test
	void doesNotHaveFactoryBeanNameShouldPass() {
		assertBeanDefinition().doesNotHaveFactoryBeanName("fooFactoryBean");
	}

	@Test
	void hasInitMethodNameShouldPass() {
		this.beanDefinition.setInitMethodName("afterPropertiesSet");
		assertBeanDefinition().hasInitMethodName("afterPropertiesSet");
	}

	@Test
	void hasInitMethodNameShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().hasInitMethodName("afterPropertiesSet"))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must equal to")
				.withMessageContaining("afterPropertiesSet");
	}

	@Test
	void doesNotHaveInitMethodNameShouldPass() {
		assertBeanDefinition().doesNotHaveInitMethodName("afterPropertiesSet");
	}

	@Test
	void doesNotHaveInitMethodNameShouldFail() {
		this.beanDefinition.setInitMethodName("afterPropertiesSet");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().doesNotHaveInitMethodName("afterPropertiesSet"))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must not equal to")
				.withMessageContaining("afterPropertiesSet");
	}

	@Test
	void hasDestroyMethodNameShouldPass() {
		this.beanDefinition.setDestroyMethodName("destroy");
		assertBeanDefinition().hasDestroyMethodName("destroy");
	}

	@Test
	void hasDestroyMethodNameShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().hasDestroyMethodName("destroy"))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must equal to")
				.withMessageContaining("destroy");
	}

	@Test
	void doesNotHaveDestroyMethodNameShouldPass() {
		assertBeanDefinition().doesNotHaveDestroyMethodName("destroy");
	}

	@Test
	void doesNotHaveDestroyMethodNameShouldFail() {
		this.beanDefinition.setDestroyMethodName("destroy");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().doesNotHaveDestroyMethodName("destroy"))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must not equal to")
				.withMessageContaining("destroy");
	}

	@Test
	void hasScopeShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertBeanDefinition().hasScope("singleton"))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must equal to")
				.withMessageContaining("singleton");
	}

	@Test
	void hasScopeShouldPass() {
		this.beanDefinition.setScope("singleton");
		assertBeanDefinition().hasScope("singleton");
	}

	@Test
	void doesNotHaveScopeShouldPass() {
		assertBeanDefinition().doesNotHaveScope("singleton");
	}

	@Test
	void doesNotHaveScopeShouldFail() {
		this.beanDefinition.setScope("singleton");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().doesNotHaveScope("singleton"))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must not equal to")
				.withMessageContaining("singleton");
	}

	@Test
	void hasRoleShouldPass() {
		this.beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		assertBeanDefinition().hasRole(BeanDefinition.ROLE_INFRASTRUCTURE);
	}

	@Test
	void hasRoleShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().hasRole(BeanDefinition.ROLE_INFRASTRUCTURE))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must equal to")
				.withMessageContaining(BeanDefinition.ROLE_INFRASTRUCTURE + "");
	}

	@Test
	void doesNotHaveRoleShouldPass() {
		assertBeanDefinition().doesNotHaveRole(BeanDefinition.ROLE_INFRASTRUCTURE);
	}

	@Test
	void doesNotHaveRoleShouldFail() {
		this.beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().doesNotHaveRole(BeanDefinition.ROLE_INFRASTRUCTURE))
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must not equal to")
				.withMessageContaining(BeanDefinition.ROLE_INFRASTRUCTURE + "");
	}

	@Test
	void isSingletonShouldPass() {
		this.beanDefinition.setScope("singleton");
		assertBeanDefinition().isSingleton();
	}

	@Test
	void isSingletonShouldFail() {
		this.beanDefinition.setScope("prototype");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertBeanDefinition().isSingleton())
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must be a singleton")
				.withMessageContaining("prototype");

	}

	@Test
	void isNotSingletonShouldPass() {
		this.beanDefinition.setScope("prototype");
		assertBeanDefinition().isNotSingleton();
	}

	@Test
	void isNotSingletonShouldFail() {
		this.beanDefinition.setScope("singleton");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertBeanDefinition().isNotSingleton())
				.withMessageContaining("Bean definition of name <foo>")
				.withMessageContaining("must not be a singleton");
	}

	@Test
	void isPrototypeShouldPass() {
		this.beanDefinition.setScope("prototype");
		assertBeanDefinition().isPrototype();
	}

	@Test
	void isPrototypeShouldFail() {
		this.beanDefinition.setScope("singleton");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertBeanDefinition().isPrototype())
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must be a prototype")
				.withMessageContaining("singleton");
	}

	@Test
	void isNotPrototypeShouldPass() {
		this.beanDefinition.setScope("singleton");
		assertBeanDefinition().isNotPrototype();
	}

	@Test
	void isNotPrototypeShouldFail() {
		this.beanDefinition.setScope("prototype");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertBeanDefinition().isNotPrototype())
				.withMessageContaining("Bean definition of name <foo>")
				.withMessageContaining("must not be a prototype");
	}

	@Test
	void isAbstractShouldPass() {
		this.beanDefinition.setAbstract(true);
		assertBeanDefinition().isAbstract();
	}

	@Test
	void isAbstractShouldFail() {
		this.beanDefinition.setAbstract(false);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertBeanDefinition().isAbstract())
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must be an abstract");
	}

	@Test
	void isNotAbstractShouldPass() {
		this.beanDefinition.setAbstract(false);
		assertBeanDefinition().isNotAbstract();
	}

	@Test
	void isNotAbstractShouldFail() {
		this.beanDefinition.setAbstract(true);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertBeanDefinition().isNotAbstract())
				.withMessageContaining("Bean definition of name <foo>")
				.withMessageContaining("must not be an abstract");
	}

	@Test
	void isLazyShouldPass() {
		this.beanDefinition.setLazyInit(true);
		assertBeanDefinition().isLazy();
	}

	@Test
	void isLazyShouldFail() {
		this.beanDefinition.setLazyInit(false);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertBeanDefinition().isLazy())
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must be a lazy");
	}

	@Test
	void isNotLazyShouldPass() {
		this.beanDefinition.setLazyInit(false);
		assertBeanDefinition().isNotLazy();
	}

	@Test
	void isNotLazyShouldFail() {
		this.beanDefinition.setLazyInit(true);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertBeanDefinition().isNotLazy())
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must not be a lazy");
	}

	@Test
	void isPrimaryShouldPass() {
		this.beanDefinition.setPrimary(true);
		assertBeanDefinition().isPrimary();
	}

	@Test
	void isPrimaryShouldFail() {
		this.beanDefinition.setPrimary(false);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertBeanDefinition().isPrimary())
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must be a primary");
	}

	@Test
	void isNotPrimaryShouldPass() {
		this.beanDefinition.setPrimary(false);
		assertBeanDefinition().isNotPrimary();
	}

	@Test
	void isNotPrimaryShouldFail() {
		this.beanDefinition.setPrimary(true);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertBeanDefinition().isNotPrimary())
				.withMessageContaining("Bean definition of name <foo>").withMessageContaining("must not be a primary");
	}

	@Test
	void isAutowireCandidateShouldPass() {
		this.beanDefinition.setAutowireCandidate(true);
		assertBeanDefinition().isAutowireCandidate();
	}

	@Test
	void isAutowireCandidateShouldFail() {
		this.beanDefinition.setAutowireCandidate(false);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertBeanDefinition().isAutowireCandidate())
				.withMessageContaining("Bean definition of name <foo>")
				.withMessageContaining("must be a candidate for getting autowired");
	}

	@Test
	void isNotAutowireCandidateShouldPass() {
		this.beanDefinition.setAutowireCandidate(false);
		assertBeanDefinition().isNotAutowireCandidate();
	}

	@Test
	void isNotAutowireCandidateShouldFail() {
		this.beanDefinition.setAutowireCandidate(true);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertBeanDefinition().isNotAutowireCandidate())
				.withMessageContaining("Bean definition of name <foo>")
				.withMessageContaining("must not be a candidate for getting autowired");
	}

	private BeanDefinitionAssert<?> assertBeanDefinition() {
		return new BeanDefinitionAssert<>(this.beanDefinition).as("Bean definition of name <foo>");
	}

}
