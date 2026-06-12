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

package org.springframework.boot.webmvc.autoconfigure.actuate.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * A {@link DelegatingWebMvcConfiguration} for the management child context that only
 * delegates to {@link WebMvcConfigurer} beans defined in the local context, ignoring any
 * such beans from the parent (main) application context.
 * <p>
 * When the management server runs on a different port, the child context has the main
 * context as its parent. {@code DelegatingWebMvcConfiguration} uses {@code @Autowired}
 * to collect all {@code WebMvcConfigurer} beans, which resolves from both the child and
 * parent bean factories. This subclass overrides the injection to filter out parent
 * configurers, preventing duplicate execution of callback methods such as
 * {@code addResourceHandlers} and {@code addViewControllers}.
 * <p>
 * Filtering is deferred to {@link InitializingBean#afterPropertiesSet()} because
 * {@code @Autowired} injection occurs during {@code populateBean()}, before the
 * {@code ApplicationContextAware} callback sets the application context. The application
 * context is guaranteed to be available by the time {@code afterPropertiesSet()} is
 * called.
 *
 * @author sawirricardo
 * @since 4.0.0
 */
class ManagementChildDelegatingWebMvcConfiguration extends DelegatingWebMvcConfiguration
		implements InitializingBean {

	@Override
	@Autowired(required = false)
	void setConfigurers(List<WebMvcConfigurer> configurers) {
		// No-op: defer to afterPropertiesSet() where ApplicationContext is available.
		// The injected list contains beans from both child and parent contexts;
		// we filter to local-only in afterPropertiesSet().
	}

	@Override
	public void afterPropertiesSet() {
		super.setConfigurers(getLocalConfigurers());
	}

	private List<WebMvcConfigurer> getLocalConfigurers() {
		ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) getApplicationContext())
			.getBeanFactory();
		String[] localNames = beanFactory.getBeanNamesForType(WebMvcConfigurer.class);
		List<WebMvcConfigurer> localConfigurers = new ArrayList<>(localNames.length);
		for (String name : localNames) {
			localConfigurers.add(beanFactory.getBean(name, WebMvcConfigurer.class));
		}
		return localConfigurers;
	}

}
