package sample.data.jpa.service;

import java.util.List;

import sample.data.jpa.domain.City;
import sample.data.jpa.domain.SpaHotel;
import sample.data.jpa.domain.SportHotel;
import sample.data.jpa.domain.SportType;

public interface CityRepositoryCustom {

  /**
   * Uses JPQL.
   *
   * @return cities with a {@link SpaHotel} or a {@link SportHotel} whose main sport is the given {@link SportType}
   */
  List<City> findAllCitiesWithSpaOrSportHotelJpa(SportType sportType);

  /**
   * Uses QueryDSL.
   *
   * @return cities with a {@link SpaHotel} or a {@link SportHotel} whose main sport is the given {@link SportType}
   */
  List<City> findAllCitiesWithSpaOrSportHotelQueryDsl(SportType sportType);

}
