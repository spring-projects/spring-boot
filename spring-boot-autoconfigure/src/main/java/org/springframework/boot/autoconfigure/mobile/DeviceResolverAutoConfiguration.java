/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.mobile;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mobile.device.DeviceHandlerMethodArgumentResolver;
import org.springframework.mobile.device.DeviceResolver;
import org.springframework.mobile.device.DeviceResolverHandlerInterceptor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Mobile's
 * {@link DeviceResolver}.
 *
 * @author Roy Clarkson
 */
@Configuration
@ConditionalOnClass({ DeviceResolverHandlerInterceptor.class,
		DeviceHandlerMethodArgumentResolver.class })
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class DeviceResolverAutoConfiguration {

	@Configuration
	@ConditionalOnWebApplication
	protected static class DeviceResolverMvcConfiguration extends WebMvcConfigurerAdapter {

		@Autowired
		private DeviceResolverHandlerInterceptor deviceResolverHandlerInterceptor;

		@Bean
		@ConditionalOnMissingBean(DeviceResolverHandlerInterceptor.class)
		public DeviceResolverHandlerInterceptor deviceResolverHandlerInterceptor() {
			return new DeviceResolverHandlerInterceptor();
		}

		@Bean
		public DeviceHandlerMethodArgumentResolver deviceHandlerMethodArgumentResolver() {
			return new DeviceHandlerMethodArgumentResolver();
		}

		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addInterceptor(this.deviceResolverHandlerInterceptor);
		}

		@Override
		public void addArgumentResolvers(
				List<HandlerMethodArgumentResolver> argumentResolvers) {
			argumentResolvers.add(deviceHandlerMethodArgumentResolver());
		}

	}

}
