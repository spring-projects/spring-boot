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

package sample.metrics.atsd.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import sample.metrics.atsd.Measured;
import sample.metrics.atsd.domain.City;

import java.util.Collections;
import java.util.List;

@Repository
@Measured
public class JdbcCityRepository implements CityRepository {
	public static final String SELECT_ALL = "select * from city";
	public static final String SELECT = "select * from city where id = :id";
	public static final String INSERT =
			"insert into city (name, state, country, map) values (:name, :state, :country, :map)";
	public static final String DELETE = "delete from city where id = :id";
	private static final RowMapper<City> MAPPER = new BeanPropertyRowMapper<City>(City.class);
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Autowired
	public void setJdbcTemplate(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<City> findAll() {
		return this.jdbcTemplate.query(SELECT_ALL, MAPPER);
	}

	@Override
	public City findById(Long id) {
		return this.jdbcTemplate.queryForObject(SELECT, Collections.singletonMap("id", id), MAPPER);
	}

	@Override
	public void save(City city) {
		this.jdbcTemplate.update(INSERT, new BeanPropertySqlParameterSource(city));
	}

	@Override
	public void remove(Long id) {
		this.jdbcTemplate.update(DELETE, Collections.singletonMap("id", id));
	}
}
