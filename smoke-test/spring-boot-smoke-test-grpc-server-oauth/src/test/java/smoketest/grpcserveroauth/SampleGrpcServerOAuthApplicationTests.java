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

package smoketest.grpcserveroauth;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
import smoketest.grpcserveroauth.proto.HelloReply;
import smoketest.grpcserveroauth.proto.HelloRequest;
import smoketest.grpcserveroauth.proto.HelloWorldGrpc.HelloWorldBlockingStub;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.grpc.client.interceptor.security.BearerTokenAuthenticationInterceptor;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.SupplierClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.function.SingletonSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "spring.grpc.server.port=0",
				"spring.grpc.client.channel.default.target=static://localhost:${local.grpc.server.port}",
				"spring.grpc.client.channel.oauth.target=static://localhost:${local.grpc.server.port}" })
@DirtiesContext
class SampleGrpcServerOAuthApplicationTests {

	@Autowired
	@Qualifier("unauthenticatedHelloWorldBlockingStub")
	private HelloWorldBlockingStub unuathenticated;

	@Autowired
	@Qualifier("oauthHelloWorldBlockingStub")
	private HelloWorldBlockingStub oauth;

	@Autowired
	private ServerReflectionStub reflection;

	@Test
	void whenUnauthenticatedStub() {
		HelloWorldBlockingStub stub = this.unuathenticated;
		assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> invoke(stub::sayHelloProfileScope))
			.satisfies(statusCode(Code.UNAUTHENTICATED));
		assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> invoke(stub::sayHelloEmailScope))
			.satisfies(statusCode(Code.UNAUTHENTICATED));
		assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> invoke(stub::sayHelloAuthenticated))
			.satisfies(statusCode(Code.UNAUTHENTICATED));
		assertCanInvokeReflection();
	}

	@Test
	void whenOAuth() {
		HelloWorldBlockingStub stub = this.oauth;
		assertThat(invoke(stub::sayHelloProfileScope)).isEqualTo("sayHelloProfileScope 'Spring'");
		assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> invoke(stub::sayHelloEmailScope))
			.satisfies(statusCode(Code.PERMISSION_DENIED));
		assertThat(invoke(stub::sayHelloAuthenticated)).isEqualTo("sayHelloAuthenticated 'Spring'");
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
	@ImportGrpcClients(target = "oauth", prefix = "oauth", types = HelloWorldBlockingStub.class)
	static class GrpcClientTestConfiguration {

		@Bean
		<B extends ManagedChannelBuilder<B>> GrpcChannelBuilderCustomizer<B> channelSecurityCustomizer(
				ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository) {
			return GrpcChannelBuilderCustomizer.matching("oauth", (builder) -> {
				Supplier<String> tokenSupplier = SingletonSupplier
					.of(() -> token(clientRegistrationRepository.getObject()));
				builder.intercept(new BearerTokenAuthenticationInterceptor(tokenSupplier));
			});
		}

		private String token(ClientRegistrationRepository clientRegistrationRepository) {
			RestClientClientCredentialsTokenResponseClient client = new RestClientClientCredentialsTokenResponseClient();
			ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("spring");
			assertThat(registration).isNotNull();
			OAuth2ClientCredentialsGrantRequest request = new OAuth2ClientCredentialsGrantRequest(registration);
			return client.getTokenResponse(request).getAccessToken().getTokenValue();
		}

		@Bean
		ClientRegistrationRepository lazyClientRegistrationRepository(Environment environment) {
			return new SupplierClientRegistrationRepository(() -> getClientRegistrationRepository(environment));
		}

		private InMemoryClientRegistrationRepository getClientRegistrationRepository(Environment environment) {
			return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("spring")
				.clientId("spring")
				.clientSecret("secret")
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope("profile")
				.tokenUri(environment.resolvePlaceholders("http://localhost:${local.server.port}/oauth2/token"))
				.build());
		}

		@Bean
		SupplierJwtDecoder lazyJwtDecoder(Environment environment) {
			return new SupplierJwtDecoder(() -> getJwtDecoder(environment));
		}

		private JwtDecoder getJwtDecoder(Environment environment) {
			JwkSetUriJwtDecoderBuilder builder = NimbusJwtDecoder
				.withJwkSetUri(environment.resolvePlaceholders("http://localhost:${local.server.port}/oauth2/jwks"));
			builder.jwsAlgorithms((algorithms) -> algorithms.add(SignatureAlgorithm.from("RS256")));
			return builder.build();
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
