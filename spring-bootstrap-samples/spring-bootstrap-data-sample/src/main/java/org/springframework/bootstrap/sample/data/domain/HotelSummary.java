package org.springframework.bootstrap.sample.data.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class HotelSummary implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final MathContext MATH_CONTEXT = new MathContext(2,
			RoundingMode.HALF_UP);

	private City city;

	private String name;

	private Double averageRating;

	private Integer averageRatingRounded;

	public HotelSummary(City city, String name, Double averageRating) {
		this.city = city;
		this.name = name;
		this.averageRating = averageRating == null ? null : new BigDecimal(averageRating,
				MATH_CONTEXT).doubleValue();
		this.averageRatingRounded = averageRating == null ? null : (int) Math
				.round(averageRating);
	}

	public City getCity() {
		return this.city;
	}

	public String getName() {
		return this.name;
	}

	public Double getAverageRating() {
		return this.averageRating;
	}

	public Integer getAverageRatingRounded() {
		return this.averageRatingRounded;
	}
}
