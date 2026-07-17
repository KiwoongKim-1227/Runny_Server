package com.goodspace.runny.domain.dog.repository;

import com.goodspace.runny.domain.dog.entity.UserDog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 보유 강아지 리포지토리. breed를 함께 조회하는 메서드는 JOIN FETCH로 N+1을 방지한다.
 */
public interface UserDogRepository extends JpaRepository<UserDog, Long> {

    /** 보유 강아지 목록 (breed FETCH JOIN) - 종 변경 화면/견종 보유 여부 조회용 */
    @Query("SELECT ud FROM UserDog ud JOIN FETCH ud.breed WHERE ud.userId = :userId ORDER BY ud.id ASC")
    List<UserDog> findByUserIdOrderByIdAsc(@Param("userId") Long userId);

    /** 단건 조회 (breed FETCH JOIN) - 활성 강아지/프로필 조회용 */
    @Query("SELECT ud FROM UserDog ud JOIN FETCH ud.breed WHERE ud.id = :id AND ud.userId = :userId")
    Optional<UserDog> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    boolean existsByUserIdAndBreedId(Long userId, Long breedId);

    /** 유저가 보유한 견종 목록 (breed FETCH JOIN) - 견종 목록의 보유 여부 표시용 */
    @Query("SELECT ud FROM UserDog ud JOIN FETCH ud.breed WHERE ud.userId = :userId")
    List<UserDog> findByUserId(@Param("userId") Long userId);

    /** 여러 강아지 일괄 조회 (breed FETCH JOIN) - UserSummaryService용 */
    @Query("SELECT ud FROM UserDog ud JOIN FETCH ud.breed WHERE ud.id IN :ids")
    List<UserDog> findAllByIdWithBreed(@Param("ids") List<Long> ids);
}
