/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.test.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A user of the system.
 *
 * @author Phillip Webb
 */
@Entity
@Table(name = "DRIVER")
public class User {

	@Id
	@GeneratedValue
	private @Nullable Long id;

	@Column(unique = true)
	private String username;

	private VehicleIdentificationNumber vin;

	protected User() {
	}

	public User(String username, VehicleIdentificationNumber vin) {
		Assert.hasLength(username, "'username' must not be empty");
		Assert.notNull(vin, "'vin' must not be null");
		this.username = username;
		this.vin = vin;
	}

	public @Nullable Long getId() {
		return this.id;
	}

	public String getUsername() {
		return this.username;
	}

	public VehicleIdentificationNumber getVin() {
		return this.vin;
	}

}
