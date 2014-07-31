/*
 * Copyright 2012-2014 the original author or authors.
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
 *
 * @author Monica Granbois
 */

package sample.data.jdbc.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class UserRepository {

	private JdbcTemplate jdbc;

	@Autowired
	public UserRepository(JdbcTemplate jdbcTemplate) {
		this.jdbc = jdbcTemplate;
	}

	public User getUser(int id) {
		return jdbc.queryForObject("SELECT * FROM APP_USERS WHERE ID=?", new RowMapper<User>() {
			public User mapRow(ResultSet rs, int rowNum) throws SQLException {
				User user = new User();
				user.setId(rs.getInt("id"));
				user.setFirstName(rs.getString("firstname"));
				user.setLastName(rs.getString("lastname"));
				return user;
			}
		}, id);
	}

	public List<User> getUsers() {
		List<User> results = new ArrayList<User>();
		List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM APP_USERS");
		for (Map row : rows) {
			User user = new User();
			user.setFirstName((String) row.get("FIRSTNAME"));
			user.setLastName((String) row.get("LASTNAME"));
			user.setId((Integer) row.get("ID"));
			results.add(user);
		}

		return results;
	}

	/**
	 * @return - the user.id value that was created by the database
	 */
	public int insertUser(User user) {
		final String firstName = user.getFirstName();
		final String lastName = user.getLastName();
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbc.update(
				new PreparedStatementCreator() {
					public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
						PreparedStatement ps =
								connection.prepareStatement("INSERT INTO APP_USERS (ID, FIRSTNAME, LASTNAME) VALUES (DEFAULT, ?, ?)", new String[]{"ID"});
						ps.setString(1, firstName);
						ps.setString(2, lastName);
						return ps;
					}
				},
				keyHolder);

		return keyHolder.getKey().intValue();
	}

}

