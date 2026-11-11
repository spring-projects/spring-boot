/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.test.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Details of a single vehicle.
 *
 * @author Phillip Webb
 */
public class VehicleDetails {

	private final String make;

	private final String model;

	@JsonCreator
	public VehicleDetails(@JsonProperty("make") String make, @JsonProperty("model") String model) {
		Assert.notNull(make, "'make' must not be null");
		Assert.notNull(model, "'model' must not be null");
		this.make = make;
		this.model = model;
	}

	public String getMake() {
		return this.make;
	}

	public String getModel() {
		return this.model;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		VehicleDetails other = (VehicleDetails) obj;
		return this.make.equals(other.make) && this.model.equals(other.model);
	}

	@Override
	public int hashCode() {
		return this.make.hashCode() * 31 + this.model.hashCode();
	}

}
