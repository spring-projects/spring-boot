/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.secure.oauth2;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Spring Data interface with secured methods
 *
 * @author Craig Walls
 * @author Greg Turnquist
 */
public interface FlightRepository extends CrudRepository<Flight, Long> {

	@Override
	@PreAuthorize("#oauth2.hasScope('read')")
	Iterable<Flight> findAll();

	@Override
	@PreAuthorize("#oauth2.hasScope('read')")
	Flight findOne(Long aLong);

	@Override
	@PreAuthorize("#oauth2.hasScope('write')")
	<S extends Flight> S save(S entity);

}
