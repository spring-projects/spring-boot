package sample.data.jpa.service;

import com.mysema.query.jpa.JPQLQueryFactory;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;

import sample.data.jpa.domain.City;
import sample.data.jpa.domain.QCity;
import sample.data.jpa.domain.QHotel;
import sample.data.jpa.domain.QSportHotel;
import sample.data.jpa.domain.SpaHotel;
import sample.data.jpa.domain.SportType;

public class CityRepositoryImpl implements CityRepositoryCustom {
  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private JPQLQueryFactory queryFactory;


  @Override
  public List<City> findAllCitiesWithSpaOrSportHotelJpa(SportType sportType) {
    String jpql = "select c from City c"
        + " join c.hotels hotel"
        + " left join hotel.mainSport sport"
        + " where (sport is null or sport.type = :sportType)";

    return entityManager.createQuery(jpql, City.class)
        .setParameter("sportType", sportType)
        .getResultList();
  }

  @Override
  public List<City> findAllCitiesWithSpaOrSportHotelQueryDsl(SportType sportType) {
    QCity city = QCity.city;
    QHotel hotel = QHotel.hotel;

    // doesn't work?
    return queryFactory.from(city)
        .join(city.hotels, hotel)
        .where(
            hotel.instanceOf(SpaHotel.class).or(
                hotel.as(QSportHotel.class).mainSport.type.eq(sportType)
            )
        ).list(city);
  }
}
