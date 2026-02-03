package com.cloudweb.repository;

import com.cloudweb.entity.Point;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointRepository extends JpaRepository<Point, Long> {
    Optional<Point> findByLatitudeAndLongitude(Double latitude, Double longitude);
    Optional<Point> findFirstByLatitudeBetweenAndLongitudeBetween(
            Double latitudeStart,
            Double latitudeEnd,
            Double longitudeStart,
            Double longitudeEnd);
}
