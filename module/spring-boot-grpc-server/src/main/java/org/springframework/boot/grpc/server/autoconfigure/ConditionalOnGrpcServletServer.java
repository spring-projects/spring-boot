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

package org.springframework.boot.grpc.server.autoconfigure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.grpc.servlet.jakarta.GrpcServlet;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that determines if the Servlet container should be
 * used to run the gRPC server. The condition matches only when the app is a servlet web
 * application and the {@code io.grpc.servlet.jakarta.GrpcServlet} class is on the
 * classpath and the {@code spring.grpc.server.servlet.enabled} property is not explicitly
 * set to {@code false}.
 *
 * @author Chris Bono
 * @author Dave Syer
 * @since 4.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(GrpcServlet.class)
@ConditionalOnProperty(prefix = "spring.grpc.server", name = "servlet.enabled", havingValue = "true",
		matchIfMissing = true)
public @interface ConditionalOnGrpcServletServer {

}
