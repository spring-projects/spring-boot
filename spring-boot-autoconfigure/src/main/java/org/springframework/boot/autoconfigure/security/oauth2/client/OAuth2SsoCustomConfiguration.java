/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2SsoCustomConfiguration.WebSecurityEnhancerCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Configuration for OAuth2 Single Sign On (SSO) when there is an existing
 * {@link WebSecurityConfigurerAdapter} provided by the user and annotated with
 * <code>@EnableOAuth2Sso</code>. The user-provided configuration is enhanced by adding an
 * authentication filter and an authentication entry point.
 * 
 * @author Dave Syer
 *
 */
@Configuration
@Conditional(WebSecurityEnhancerCondition.class)
public class OAuth2SsoCustomConfiguration implements ImportAware, BeanPostProcessor,
		BeanFactoryAware {

	private Class<?> configType;

	private BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		configType = ClassUtils.resolveClassName(importMetadata.getClassName(), null);

	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		if (configType.isAssignableFrom(bean.getClass())
				&& bean instanceof WebSecurityConfigurerAdapter) {
			ProxyFactory factory = new ProxyFactory();
			factory.setTarget(bean);
			factory.addAdvice(new SsoSecurityAdapter(beanFactory));
			bean = factory.getProxy();
		}
		return bean;
	}

	private static class SsoSecurityAdapter implements MethodInterceptor {

		private SsoSecurityConfigurer configurer;

		public SsoSecurityAdapter(BeanFactory beanFactory) {
			configurer = new SsoSecurityConfigurer(beanFactory);
		}

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			if (invocation.getMethod().getName().equals("init")) {
				Method method = ReflectionUtils.findMethod(
						WebSecurityConfigurerAdapter.class, "getHttp");
				ReflectionUtils.makeAccessible(method);
				HttpSecurity http = (HttpSecurity) ReflectionUtils.invokeMethod(method,
						(WebSecurityConfigurerAdapter) invocation.getThis());
				configurer.configure(http);
			}
			return invocation.proceed();
		}

	}

	protected static class WebSecurityEnhancerCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			String[] enablers = context.getBeanFactory().getBeanNamesForAnnotation(
					EnableOAuth2Sso.class);
			for (String name : enablers) {
				if (context.getBeanFactory().isTypeMatch(name,
						WebSecurityConfigurerAdapter.class)) {
					return ConditionOutcome
							.match("found @EnableOAuth2Sso on a WebSecurityConfigurerAdapter");
				}
			}
			return ConditionOutcome
					.noMatch("found no @EnableOAuth2Sso on a WebSecurityConfigurerAdapter");
		}
	}

}
