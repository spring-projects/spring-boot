/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.test.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import smoketest.test.domain.VehicleIdentificationNumber;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

/**
 * {@link VehicleDetailsService} backed by a remote REST service.
 *
 * @author Phillip Webb
 */
@Service
public class RemoteVehicleDetailsService implements VehicleDetailsService {

	private static final Log logger = LogFactory.getLog(RemoteVehicleDetailsService.class);

	private final RestClient restClient;

	public RemoteVehicleDetailsService(ServiceProperties properties, RestClient.Builder restTemplateBuilder) {
		this.restClient = restTemplateBuilder.baseUrl(properties.getVehicleServiceRootUrl()).build();
	}

	@Override
	public VehicleDetails getVehicleDetails(VehicleIdentificationNumber vin)
			throws VehicleIdentificationNumberNotFoundException {
		Assert.notNull(vin, "'vin' must not be null");
		logger.debug("Retrieving vehicle data for: " + vin);
		try {
			VehicleDetails response = this.restClient.get()
				.uri("/vehicle/{vin}/details", vin)
				.retrieve()
				.body(VehicleDetails.class);
			Assert.state(response != null, "'response' must not be null");
			return response;
		}
		catch (HttpStatusCodeException ex) {
			if (HttpStatus.NOT_FOUND.equals(ex.getStatusCode())) {
				throw new VehicleIdentificationNumberNotFoundException(vin, ex);
			}
			throw ex;
		}
	}

}
