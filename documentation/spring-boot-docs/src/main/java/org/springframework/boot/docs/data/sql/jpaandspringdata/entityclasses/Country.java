/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.sql.jpaandspringdata.entityclasses;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.envers.Audited;

@Entity
public class Country implements Serializable {

	@Id
	@GeneratedValue
	private Long id;

	@Audited
	@Column(nullable = false)
	private String name;

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
