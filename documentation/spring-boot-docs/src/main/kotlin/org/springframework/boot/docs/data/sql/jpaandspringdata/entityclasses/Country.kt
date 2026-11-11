/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.sql.jpaandspringdata.entityclasses

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import org.hibernate.envers.Audited
import java.io.Serializable

@Entity
class Country : Serializable {

	@Id
	@GeneratedValue
	var id: Long? = null

	@Audited
	@Column(nullable = false)
	var name: String? = null

}

