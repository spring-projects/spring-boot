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

package org.springframework.boot.autoconfigure.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.WebSecurity;

/**
 * Configuration for a Spring Security in-memory {@link AuthenticationManager}.
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnBean(ObjectPostProcessor.class)
@ConditionalOnMissingBean(AuthenticationManager.class)
@ConditionalOnWebApplication
@Order(Ordered.LOWEST_PRECEDENCE - 3)
public class AuthenticationManagerConfiguration implements
		WebSecurityConfigurer<WebSecurity> {

	@Autowired
	private SecurityProperties security;

	@Autowired
	private List<SecurityPrequisite> dependencies;

	@Override
	public void init(WebSecurity builder) throws Exception {
	}

	@Override
	public void configure(WebSecurity builder) throws Exception {
	}

	@Autowired
	public void authentication(AuthenticationManagerBuilder builder) throws Exception {
		SecurityAutoConfiguration.authentication(builder, this.security);
	}

}
