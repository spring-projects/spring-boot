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

import io.grpc.servlet.jakarta.GrpcServlet;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that determines if the gRPC server implementation
 * should be one of the native varieties (e.g. Netty, Shaded Netty) - i.e. not the servlet
 * container. The condition matches when the app is not a Reactive web application OR the
 * {@code io.grpc.servlet.jakarta.GrpcServlet} class is not on the classpath OR the app is
 * a servlet web application and the {@code io.grpc.servlet.jakarta.GrpcServlet} is on the
 * classpath BUT the {@code spring.grpc.server.servlet.enabled} property is explicitly set
 * to {@code false}.
 *
 * @author Dave Syer
 * @author Chris Bono
 */
class OnGrpcNativeServerCondition extends AnyNestedCondition {

	OnGrpcNativeServerCondition() {
		super(ConfigurationPhase.PARSE_CONFIGURATION);
	}

	@ConditionalOnNotWebApplication
	static class OnNonWebApplication {

	}

	@ConditionalOnMissingClass("io.grpc.servlet.jakarta.GrpcServlet")
	static class OnGrpcServletClass {

	}

	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	@ConditionalOnClass(GrpcServlet.class)
	@ConditionalOnProperty(prefix = "spring.grpc.server", name = "servlet.enabled", havingValue = "false")
	static class OnExplicitlyDisabledServlet {

	}

	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
	static class OnExplicitlyDisabledWebflux {

	}

}
