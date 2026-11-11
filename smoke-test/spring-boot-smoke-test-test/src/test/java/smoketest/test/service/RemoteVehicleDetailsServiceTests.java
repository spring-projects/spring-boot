/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.test.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import smoketest.test.domain.VehicleIdentificationNumber;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link RemoteVehicleDetailsService}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@RestClientTest
@TestInstance(Lifecycle.PER_CLASS)
class RemoteVehicleDetailsServiceTests {

	private static final String VIN = "00000000000000000";

	private static final String BASE_URL = "https://api.example.com";

	private final RemoteVehicleDetailsService service;

	private final MockRestServiceServer server;

	RemoteVehicleDetailsServiceTests(@Autowired RestClient.Builder restClientBuilder,
			@Autowired MockRestServiceServer server) {
		ServiceProperties properties = new ServiceProperties();
		properties.setVehicleServiceRootUrl(BASE_URL);
		this.service = new RemoteVehicleDetailsService(properties, restClientBuilder);
		this.server = server;
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void getVehicleDetailsWhenVinIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.service.getVehicleDetails(null))
			.withMessage("'vin' must not be null");
	}

	@Test
	void getVehicleDetailsWhenResultIsSuccessShouldReturnDetails() {
		this.server.expect(prepareRequest("/vehicle/" + VIN + "/details"))
			.andRespond(withSuccess(getClassPathResource("vehicledetails.json"), MediaType.APPLICATION_JSON));
		VehicleDetails details = this.service.getVehicleDetails(new VehicleIdentificationNumber(VIN));
		assertThat(details.getMake()).isEqualTo("Honda");
		assertThat(details.getModel()).isEqualTo("Civic");
	}

	@Test
	void getVehicleDetailsWhenResultIsNotFoundShouldThrowException() {
		this.server.expect(prepareRequest("/vehicle/" + VIN + "/details")).andRespond(withStatus(HttpStatus.NOT_FOUND));
		assertThatExceptionOfType(VehicleIdentificationNumberNotFoundException.class)
			.isThrownBy(() -> this.service.getVehicleDetails(new VehicleIdentificationNumber(VIN)));
	}

	@Test
	void getVehicleDetailsWhenResultIServerErrorShouldThrowException() {
		this.server.expect(prepareRequest("/vehicle/" + VIN + "/details")).andRespond(withServerError());
		assertThatExceptionOfType(HttpServerErrorException.class)
			.isThrownBy(() -> this.service.getVehicleDetails(new VehicleIdentificationNumber(VIN)));
	}

	private static RequestMatcher prepareRequest(String path) {
		return requestTo(BASE_URL + path);
	}

	private ClassPathResource getClassPathResource(String path) {
		return new ClassPathResource(path, getClass());
	}

}
