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

package smoketest.grpcserversecure;

import java.util.function.Consumer;
import java.util.function.Function;

import io.grpc.ManagedChannelBuilder;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.reflection.v1.ServerReflectionGrpc.ServerReflectionStub;
import io.grpc.reflection.v1.ServerReflectionRequest;
import io.grpc.reflection.v1.ServerReflectionResponse;
import io.grpc.stub.StreamObserver;
import org.awaitility.Awaitility;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import smoketest.grpcserversecure.proto.HelloReply;
import smoketest.grpcserversecure.proto.HelloRequest;
import smoketest.grpcserversecure.proto.HelloWorldGrpc.HelloWorldBlockingStub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.grpc.client.interceptor.security.BasicAuthenticationInterceptor;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest(properties = { "spring.grpc.server.port=0",
		"spring.grpc.client.channel.default.target=static://localhost:${local.grpc.server.port}",
		"spring.grpc.client.channel.user.target=static://localhost:${local.grpc.server.port}",
		"spring.grpc.client.channel.admin.target=static://localhost:${local.grpc.server.port}" })
@DirtiesContext
class SampleGrpcServerSecureApplicationTests {

	@Autowired
	@Qualifier("unauthenticatedHelloWorldBlockingStub")
	private HelloWorldBlockingStub unuathenticated;

	@Autowired
	@Qualifier("userAuthenticatedHelloWorldBlockingStub")
	private HelloWorldBlockingStub userAuthenticated;

	@Autowired
	@Qualifier("adminAuthenticatedHelloWorldBlockingStub")
	private HelloWorldBlockingStub adminAuthenticated;

	@Autowired
	private ServerReflectionStub reflection;

	@Test
	void whenUnauthenticatedStub() {
		HelloWorldBlockingStub stub = this.unuathenticated;
		assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> invoke(stub::sayHelloUser))
			.satisfies(statusCode(Code.UNAUTHENTICATED));
		assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> invoke(stub::sayHelloUserAnnotated))
			.satisfies(statusCode(Code.UNAUTHENTICATED));
		assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> invoke(stub::sayHelloAdmin))
			.satisfies(statusCode(Code.UNAUTHENTICATED));
		assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> invoke(stub::sayHelloAdminAnnotated))
			.satisfies(statusCode(Code.UNAUTHENTICATED));
		assertCanInvokeReflection();
	}

	@Test
	void whenUserAuthenticatedStub() {
		HelloWorldBlockingStub stub = this.userAuthenticated;
		assertThat(invoke(stub::sayHelloUser)).isEqualTo("sayHelloUser 'Spring'");
		assertThat(invoke(stub::sayHelloUserAnnotated)).isEqualTo("sayHelloUserAnnotated 'Spring'");
		assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> invoke(stub::sayHelloAdmin))
			.satisfies(statusCode(Code.PERMISSION_DENIED));
		assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> invoke(stub::sayHelloAdminAnnotated))
			.satisfies(statusCode(Code.PERMISSION_DENIED));
		assertCanInvokeReflection();
	}

	@Test
	void whenAdminAuthenticatedStub() {
		HelloWorldBlockingStub stub = this.adminAuthenticated;
		assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> invoke(stub::sayHelloUser))
			.satisfies(statusCode(Code.PERMISSION_DENIED));
		assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> invoke(stub::sayHelloUserAnnotated))
			.satisfies(statusCode(Code.PERMISSION_DENIED));
		assertThat(invoke(stub::sayHelloAdmin)).isEqualTo("sayHelloAdmin 'Spring'");
		assertThat(invoke(stub::sayHelloAdminAnnotated)).isEqualTo("sayHelloAdminAnnotated 'Spring'");
		assertCanInvokeReflection();
	}

	private String invoke(Function<HelloRequest, HelloReply> method) {
		HelloRequest request = HelloRequest.newBuilder().setName("Spring").build();
		return method.apply(request).getMessage();
	}

	private void assertCanInvokeReflection() {
		ObservedResponse<ServerReflectionResponse> response = invokeReflection();
		assertThat(response.getValue()).isNotNull();
		assertThat(response.getError()).isNull();
	}

	private ObservedResponse<ServerReflectionResponse> invokeReflection() {
		ObservedResponse<ServerReflectionResponse> response = new ObservedResponse<>();
		StreamObserver<ServerReflectionRequest> request = this.reflection.serverReflectionInfo(response);
		request.onNext(ServerReflectionRequest.newBuilder().setListServices("").build());
		request.onCompleted();
		response.await();
		return response;
	}

	private Consumer<StatusRuntimeException> statusCode(Code expected) {
		return (ex) -> assertThat(ex).extracting("status.code").isEqualTo(expected);
	}

	@TestConfiguration(proxyBeanMethods = false)
	@ImportGrpcClients(types = ServerReflectionStub.class)
	@ImportGrpcClients(prefix = "unauthenticated", types = HelloWorldBlockingStub.class)
	@ImportGrpcClients(target = "user", prefix = "userAuthenticated", types = HelloWorldBlockingStub.class)
	@ImportGrpcClients(target = "admin", prefix = "adminAuthenticated", types = HelloWorldBlockingStub.class)
	static class GrpcClientTestConfiguration {

		@Bean
		<B extends ManagedChannelBuilder<B>> GrpcChannelBuilderCustomizer<B> channelSecurityCustomizer() {
			return (target, builder) -> {
				if ("user".equals(target)) {
					builder.intercept(new BasicAuthenticationInterceptor("user", "userpassword"));
				}
				if ("admin".equals(target)) {
					builder.intercept(new BasicAuthenticationInterceptor("admin", "adminpassword"));
				}
			};
		}

	}

	static class ObservedResponse<T> implements StreamObserver<T> {

		private volatile @Nullable T value;

		private volatile @Nullable Throwable error;

		@Override
		public synchronized void onNext(T value) {
			this.value = value;
		}

		@Override
		public synchronized void onError(Throwable error) {
			this.error = error;
		}

		@Override
		public void onCompleted() {
		}

		void await() {
			Awaitility.await().until(this::hasResponse);
		}

		private synchronized boolean hasResponse() {
			return this.value != null || this.error != null;
		}

		@Nullable T getValue() {
			return this.value;
		}

		@Nullable Throwable getError() {
			return this.error;
		}

	}

}
