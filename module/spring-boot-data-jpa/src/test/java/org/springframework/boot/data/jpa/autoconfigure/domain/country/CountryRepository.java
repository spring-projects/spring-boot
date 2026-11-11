/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jpa.autoconfigure.domain.country;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;

public interface CountryRepository extends JpaRepository<Country, Long>, RevisionRepository<Country, Long, Integer> {

}
