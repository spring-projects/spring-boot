/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.env.Environment;

/**
 * {@link ApplicationContextInitializer} that sets {@link Environment} properties for the
 * ports that {@link EmbeddedServletContainer} servers are actually listening on. The
 * property {@literal "local.server.port"} can be injected directly into tests using
 * {@link Value @Value} or obtained via the {@link Environment}.
 * <p>
 * If the {@link EmbeddedWebApplicationContext} has a
 * {@link EmbeddedWebApplicationContext#setNamespace(String) namespace} set, it will be
 * used to construct the property name. For example, the "management" actuator context
 * will have the property name {@literal "local.management.port"}.
 * <p>
 * Properties are automatically propagated up to any parent context.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.3.0
 * @deprecated as of 1.4 in favor of
 * org.springframework.boot.context.embedded.ServerPortInfoApplicationContextInitializer
 */
@Deprecated
public class ServerPortInfoApplicationContextInitializer extends
		org.springframework.boot.context.embedded.ServerPortInfoApplicationContextInitializer {

}
