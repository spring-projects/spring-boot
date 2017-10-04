/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.actuate.endpoint.DefaultEnablement;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional} that checks whether an endpoint is enabled or not. Matches
 * according to the {@code defaultEnablement} and {@code types} flag that the
 * {@link Endpoint} may be restricted to.
 * <p>
 * When an endpoint uses {@link DefaultEnablement#DISABLED}, it will only be enabled if
 * {@code endpoint.<name>.enabled}, {@code endpoint.<name>.jmx.enabled} or
 * {@code endpoint.<name>.web.enabled} is {@code true}.
 * <p>
 * When an endpoint uses {@link DefaultEnablement#ENABLED}, it will be enabled unless
 * {@code endpoint.<name>.enabled}, {@code endpoint.<name>.jmx.enabled} or
 * {@code endpoint.<name>.web.enabled} is {@code false}.
 * <p>
 * When an endpoint uses {@link DefaultEnablement#NEUTRAL}, it will be enabled if
 * {@code endpoint.default.enabled}, {@code endpoint.default.jmx.enabled} or
 * {@code endpoint.default.web.enabled} is {@code true} and
 * {@code endpoint.<name>.enabled}, {@code endpoint.<name>.jmx.enabled} or
 * {@code endpoint.<name>.web.enabled} has not been set to {@code false}.
 * <p>
 * If any properties are set, they are evaluated from most to least specific, e.g.
 * considering a web endpoint with id {@code foo}:
 * <ol>
 * <li>endpoints.foo.web.enabled</li>
 * <li>endpoints.foo.enabled</li>
 * <li>endpoints.default.web.enabled</li>
 * <li>endpoints.default.enabled</li>
 * </ol>
 * For instance if {@code endpoints.default.enabled} is {@code false} but
 * {@code endpoints.<name>.enabled} is {@code true}, the condition will match.
 * <p>
 * This condition must be placed on a {@code @Bean} method producing an endpoint as its id
 * and other attributes are inferred from the {@link Endpoint} annotation set on the
 * return type of the factory method. Consider the following valid example:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class MyAutoConfiguration {
 *
 *     &#064;ConditionalOnEnabledEndpoint
 *     &#064;Bean
 *     public MyEndpoint myEndpoint() {
 *         ...
 *     }
 *
 *     &#064;Endpoint(id = "my", defaultEnablement = DefaultEnablement.DISABLED)
 *     static class MyEndpoint { ... }
 *
 * }</pre>
 * <p>
 *
 * In the sample above the condition will be evaluated with the attributes specified on
 * {@code MyEndpoint}. In particular, in the absence of any property in the environment,
 * the condition will not match as this endpoint is disabled by default.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 * @see Endpoint
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
@Conditional(OnEnabledEndpointCondition.class)
public @interface ConditionalOnEnabledEndpoint {

}
