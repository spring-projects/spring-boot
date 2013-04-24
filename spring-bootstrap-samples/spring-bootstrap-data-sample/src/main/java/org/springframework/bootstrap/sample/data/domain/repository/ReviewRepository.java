package org.springframework.bootstrap.sample.data.domain.repository;

import org.springframework.bootstrap.sample.data.domain.Hotel;
import org.springframework.bootstrap.sample.data.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface ReviewRepository extends Repository<Review, Long> {

	Page<Review> findByHotel(Hotel hotel, Pageable pageable);

	Review findByHotelAndIndex(Hotel hotel, int index);

}
