/*
 * Copyright 2012-2018 the original author or authors.
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

package sample.data.gemfire.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;

import sample.data.gemfire.domain.Gemstone;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * The GemstoneServiceImpl class is a Service object implementing the GemstoneService
 * interface containing business logic and rules in addition to data services for
 * processing Gemstones.
 *
 * @author John Blum
 */
@Service("gemstoneService")
public class GemstoneServiceImpl implements GemstoneService {

	protected static final List<String> APPROVED_GEMS;

	static {
		APPROVED_GEMS = Collections.unmodifiableList(
				Arrays.asList(("ALEXANDRITE,AQUAMARINE,DIAMOND,OPAL,PEARL,"
						+ "RUBY,SAPPHIRE,SPINEL,TOPAZ").split(",")));
	}

	private final GemstoneRepository repository;

	public GemstoneServiceImpl(GemstoneRepository gemstoneRepository) {
		this.repository = gemstoneRepository;
	}

	@PostConstruct
	public void init() {
		System.out.printf("[%1$s] initialized!%n", getClass().getSimpleName());
	}

	/**
	 * Returns a count of the number of Gemstones in the GemFire Cache.
	 * @return a long value indicating the number of Gemstones in the GemFire Cache.
	 */
	@Override
	@Transactional(readOnly = true)
	public long count() {
		return this.repository.count();
	}

	/**
	 * Gets a Gemstone by ID.
	 * @param id a long value indicating the identifier of the Gemstone.
	 * @return a Gemstone with ID, or null if no Gemstone exists with ID.
	 * @see sample.data.gemfire.domain.Gemstone
	 */
	@Override
	@Transactional(readOnly = true)
	public Gemstone get(Long id) {
		return this.repository.findOne(id);
	}

	/**
	 * Gets a Gemstone by name.
	 * @param name a String value indicating the name of the Gemstone.
	 * @return a Gemstone with name, or null if no Gemstone exists with name.
	 * @see sample.data.gemfire.domain.Gemstone
	 */
	@Override
	@Transactional(readOnly = true)
	public Gemstone get(String name) {
		return this.repository.findByName(name);
	}

	/**
	 * Return a listing of Gemstones currently stored in the GemFire Cache.
	 * @return an Iterable object to iterate over the list of Gemstones currently stored
	 * in the GemFire Cache.
	 * @see java.lang.Iterable
	 * @see sample.data.gemfire.domain.Gemstone
	 */
	@Override
	@Transactional(readOnly = true)
	public Iterable<Gemstone> list() {
		return this.repository.findAll();
	}

	/**
	 * Saves the specified Gemstone to the GemFire Cache.
	 * @param gemstone the Gemstone to save in the GemFire Cache.
	 * @return the saved Gemstone.
	 * @see sample.data.gemfire.domain.Gemstone
	 */
	@Override
	@Transactional
	public Gemstone save(Gemstone gemstone) {
		Assert.notNull(gemstone, "The Gemstone to save must not be null!");
		Assert.notNull(gemstone.getName(), "The name of the Gemstone must be specified!");
		// NOTE deliberately (& naively) validate the Gemstone after mutating data access
		// in GemFire rather than before to demonstrate transactions in GemFire.
		Gemstone savedGemstone = validate(this.repository.save(gemstone));
		Assert.state(savedGemstone.equals(get(gemstone.getId())),
				String.format("Failed to find Gemstone (%1$s) in "
						+ "GemFire's Cache Region 'Gemstones'!", gemstone));
		System.out.printf("Saved Gemstone [%1$s]%n", savedGemstone.getName());
		return gemstone;
	}

	Gemstone validate(Gemstone gemstone) {
		if (!APPROVED_GEMS.contains(gemstone.getName().toUpperCase(Locale.ENGLISH))) {
			// NOTE if the Gemstone is not valid, throw error...
			// Should cause transaction to rollback in GemFire!
			System.err.printf("Illegal Gemstone [%1$s]!%n", gemstone.getName());
			throw new IllegalGemstoneException(
					String.format("[%1$s] is not a valid Gemstone!", gemstone.getName()));
		}
		return gemstone;
	}

}
