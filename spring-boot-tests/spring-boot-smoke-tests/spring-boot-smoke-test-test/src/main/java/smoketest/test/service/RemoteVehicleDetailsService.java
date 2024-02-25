/*
 * Copyright 2012-2019 the original author or authors.
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

package smoketest.test.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import smoketest.test.domain.VehicleIdentificationNumber;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * {@link VehicleDetailsService} backed by a remote REST service.
 *
 * @author Phillip Webb
 */
@Service
public class RemoteVehicleDetailsService implements VehicleDetailsService {

	private static final Log logger = LogFactory.getLog(RemoteVehicleDetailsService.class);

	private final RestTemplate restTemplate;

	/**
     * Constructs a new RemoteVehicleDetailsService with the specified ServiceProperties and RestTemplateBuilder.
     * 
     * @param properties the ServiceProperties object containing the root URL of the vehicle service
     * @param restTemplateBuilder the RestTemplateBuilder used to build the RestTemplate for making HTTP requests
     */
    public RemoteVehicleDetailsService(ServiceProperties properties, RestTemplateBuilder restTemplateBuilder) {
		this.restTemplate = restTemplateBuilder.rootUri(properties.getVehicleServiceRootUrl()).build();
	}

	/**
     * Retrieves the details of a vehicle based on its Vehicle Identification Number (VIN).
     * 
     * @param vin The Vehicle Identification Number (VIN) of the vehicle.
     * @return The VehicleDetails object containing the details of the vehicle.
     * @throws VehicleIdentificationNumberNotFoundException If the VIN is not found in the database.
     * @throws IllegalArgumentException If the VIN is null.
     */
    @Override
	public VehicleDetails getVehicleDetails(VehicleIdentificationNumber vin)
			throws VehicleIdentificationNumberNotFoundException {
		Assert.notNull(vin, "VIN must not be null");
		logger.debug("Retrieving vehicle data for: " + vin);
		try {
			return this.restTemplate.getForObject("/vehicle/{vin}/details", VehicleDetails.class, vin);
		}
		catch (HttpStatusCodeException ex) {
			if (HttpStatus.NOT_FOUND.equals(ex.getStatusCode())) {
				throw new VehicleIdentificationNumberNotFoundException(vin, ex);
			}
			throw ex;
		}
	}

}
