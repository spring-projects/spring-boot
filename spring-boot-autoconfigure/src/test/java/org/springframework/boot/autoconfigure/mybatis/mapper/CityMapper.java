package org.springframework.boot.autoconfigure.mybatis.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.boot.autoconfigure.mybatis.domain.City;

public interface CityMapper {

	@Select("SELECT * FROM city WHERE id = #{cityId}")
	City findById(@Param("cityId") Long cityId);

}
