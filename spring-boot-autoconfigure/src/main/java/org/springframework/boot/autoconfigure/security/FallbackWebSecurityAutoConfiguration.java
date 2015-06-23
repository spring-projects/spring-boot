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

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * If the user explicitly disables the basic security features and forgets to
 * {@code @EnableWebSecurity}, and yet still wants a bean of type
 * WebSecurityConfigurerAdapter, he is trying to use a custom security setup. The app
 * would fail in a confusing way without this shim configuration, which just helpfully
 * defines an empty {@code @EnableWebSecurity}.
 *
 * @author Dave Syer
 */
@ConditionalOnProperty(prefix = "security.basic", name = "enabled", havingValue = "false")
@ConditionalOnBean(WebSecurityConfigurerAdapter.class)
@ConditionalOnClass(EnableWebSecurity.class)
@ConditionalOnMissingBean(WebSecurityConfiguration.class)
@ConditionalOnWebApplication
@AutoConfigureAfter(SecurityAutoConfiguration.class)
@EnableWebSecurity
public class FallbackWebSecurityAutoConfiguration {
}
