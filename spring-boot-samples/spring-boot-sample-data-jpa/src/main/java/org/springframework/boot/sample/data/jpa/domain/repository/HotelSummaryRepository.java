/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.sample.data.jpa.domain.repository;

import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.boot.sample.data.jpa.domain.City;
import org.springframework.boot.sample.data.jpa.domain.Hotel;
import org.springframework.boot.sample.data.jpa.domain.HotelSummary;
import org.springframework.boot.sample.data.jpa.domain.RatingCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Repository;

@Repository
public class HotelSummaryRepository {

	private static final String AVERAGE_REVIEW_FUNCTION = "avg(r.rating)";

	private static final String FIND_BY_CITY_QUERY = "select new "
			+ HotelSummary.class.getName() + "(h.city, h.name, "
			+ AVERAGE_REVIEW_FUNCTION
			+ ") from Hotel h left outer join h.reviews r where h.city = ?1 group by h";

	private static final String FIND_BY_CITY_COUNT_QUERY = "select count(h) from Hotel h where h.city = ?1";

	private static final String FIND_RATING_COUNTS_QUERY = "select new "
			+ RatingCount.class.getName() + "(r.rating, count(r)) "
			+ "from Review r where r.hotel = ?1 group by r.rating order by r.rating DESC";

	private EntityManager entityManager;

	public Page<HotelSummary> findByCity(City city, Pageable pageable) {
		StringBuilder queryString = new StringBuilder(FIND_BY_CITY_QUERY);
		applySorting(queryString, pageable == null ? null : pageable.getSort());

		Query query = this.entityManager.createQuery(queryString.toString());
		query.setParameter(1, city);
		query.setFirstResult(pageable.getOffset());
		query.setMaxResults(pageable.getPageSize());

		Query countQuery = this.entityManager.createQuery(FIND_BY_CITY_COUNT_QUERY);
		countQuery.setParameter(1, city);

		@SuppressWarnings("unchecked")
		List<HotelSummary> content = query.getResultList();

		Long total = (Long) countQuery.getSingleResult();

		return new PageImpl<HotelSummary>(content, pageable, total);
	}

	@SuppressWarnings("unchecked")
	public List<RatingCount> findRatingCounts(Hotel hotel) {
		Query query = this.entityManager.createQuery(FIND_RATING_COUNTS_QUERY);
		query.setParameter(1, hotel);
		return query.getResultList();
	}

	private void applySorting(StringBuilder query, Sort sort) {
		if (sort != null) {
			query.append(" order by");
			for (Order order : sort) {
				String aliasedProperty = getAliasedProperty(order.getProperty());
				query.append(String.format(" %s %s,", aliasedProperty, order
						.getDirection().name().toLowerCase(Locale.US)));
			}
			query.deleteCharAt(query.length() - 1);
		}
	}

	private String getAliasedProperty(String property) {
		if (property.equals("averageRating")) {
			return AVERAGE_REVIEW_FUNCTION;
		}
		return "h." + property;
	}

	@PersistenceContext
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

}
