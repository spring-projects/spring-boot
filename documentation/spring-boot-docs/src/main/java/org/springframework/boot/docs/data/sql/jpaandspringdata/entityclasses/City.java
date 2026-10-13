/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.sql.jpaandspringdata.entityclasses;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class City implements Serializable {

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String state;

	// ... additional members, often include @OneToMany mappings

	protected City() {
		// no-args constructor required by JPA spec
		// this one is protected since it should not be used directly
	}

	public City(String name, String state) {
		this.name = name;
		this.state = state;
	}

	public String getName() {
		return this.name;
	}

	public String getState() {
		return this.state;
	}

	// ... etc

}
