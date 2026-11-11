/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.ldap.autoconfigure.domain.person;

import javax.naming.Name;

import org.springframework.data.repository.Repository;

public interface PersonRepository extends Repository<Person, Name> {

}
