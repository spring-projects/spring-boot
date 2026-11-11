/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.jpa.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.annotations.NaturalId;

@Entity
public class Hotel implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "hotel_generator", sequenceName = "hotel_sequence", initialValue = 28)
	@GeneratedValue(generator = "hotel_generator")
	@SuppressWarnings("NullAway.Init")
	private Long id;

	@ManyToOne(optional = false)
	@NaturalId
	@SuppressWarnings("NullAway.Init")
	private City city;

	@Column(nullable = false)
	@NaturalId
	@SuppressWarnings("NullAway.Init")
	private String name;

	@Column(nullable = false)
	@SuppressWarnings("NullAway.Init")
	private String address;

	@Column(nullable = false)
	@SuppressWarnings("NullAway.Init")
	private String zip;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "hotel")
	private Set<Review> reviews = new HashSet<>();

	protected Hotel() {
	}

	public Hotel(City city, String name, String address, String zip) {
		this.city = city;
		this.name = name;
		this.address = address;
		this.zip = zip;
	}

	public City getCity() {
		return this.city;
	}

	public String getName() {
		return this.name;
	}

	public String getAddress() {
		return this.address;
	}

	public String getZip() {
		return this.zip;
	}

}
