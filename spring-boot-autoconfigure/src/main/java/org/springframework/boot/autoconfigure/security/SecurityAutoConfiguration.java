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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.AbstractConfiguredSecurityBuilder;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.util.ReflectionUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Security. Provides an
 * {@link AuthenticationManager} based on configuration bound to a
 * {@link SecurityProperties} bean. There is one user (named "user") whose password is
 * random and printed on the console at INFO level during startup. In a webapp this
 * configuration also secures all web endpoints (except some well-known static resource)
 * locations with HTTP basic security. To replace all the default behaviour in a webapp
 * provide a <code>@Configuration</code> with <code>@EnableWebSecurity</code>. To just add
 * your own layer of application security in front of the defaults, add a
 * <code>@Configuration</code> of type {@link WebSecurityConfigurerAdapter}.
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass(AuthenticationManager.class)
@EnableConfigurationProperties
@Import({ SpringBootWebSecurityConfiguration.class,
		AuthenticationManagerConfiguration.class })
public class SecurityAutoConfiguration {

	private static Log logger = LogFactory.getLog(SecurityAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	public SecurityProperties securityProperties() {
		return new SecurityProperties();
	}

	@Bean
	@ConditionalOnBean(AuthenticationManagerBuilder.class)
	@ConditionalOnMissingBean
	public AuthenticationManager authenticationManager(
			AuthenticationManagerBuilder builder, ObjectPostProcessor<Object> processor)
			throws Exception {
		if (!isBuilt(builder)) {
			authentication(builder, securityProperties());
		}
		else if (builder.getOrBuild() == null) {
			builder = new AuthenticationManagerBuilder(processor);
			authentication(builder, securityProperties());
		}
		return builder.getOrBuild();
	}

	/**
	 * Convenience method for building the default AuthenticationManager from
	 * SecurityProperties.
	 * 
	 * @param builder the AuthenticationManagerBuilder to use
	 * @param security the SecurityProperties in use
	 */
	public static void authentication(AuthenticationManagerBuilder builder,
			SecurityProperties security) throws Exception {

		if (isBuilt(builder)) {
			return;
		}

		User user = security.getUser();

		if (user.isDefaultPassword()) {
			logger.info("\n\nUsing default password for application endpoints: "
					+ user.getPassword() + "\n\n");
		}

		Set<String> roles = new LinkedHashSet<String>(user.getRole());

		builder.inMemoryAuthentication().withUser(user.getName())
				.password(user.getPassword())
				.roles(roles.toArray(new String[roles.size()]));

	}

	private static boolean isBuilt(AuthenticationManagerBuilder builder) {
		Method configurers = ReflectionUtils.findMethod(
				AbstractConfiguredSecurityBuilder.class, "getConfigurers");
		Method unbuilt = ReflectionUtils.findMethod(
				AbstractConfiguredSecurityBuilder.class, "isUnbuilt");
		ReflectionUtils.makeAccessible(configurers);
		ReflectionUtils.makeAccessible(unbuilt);
		return !((Collection<?>) ReflectionUtils.invokeMethod(configurers, builder))
				.isEmpty() || !((Boolean) ReflectionUtils.invokeMethod(unbuilt, builder));
	}
}
