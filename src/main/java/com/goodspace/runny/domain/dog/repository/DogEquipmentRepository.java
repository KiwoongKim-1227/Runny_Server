package com.goodspace.runny.domain.dog.repository;

import com.goodspace.runny.domain.dog.entity.DogEquipment;
import com.goodspace.runny.domain.item.entity.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 강아지 착용 코디 리포지토리. 실제 착용 로직은 4단계 드레스룸에서 사용한다.
 */
public interface DogEquipmentRepository extends JpaRepository<DogEquipment, Long> {

    List<DogEquipment> findByUserDogId(Long userDogId);

    /** 여러 강아지의 착용 코디 일괄 조회 (UserSummary 조립용) */
    List<DogEquipment> findByUserDogIdIn(List<Long> userDogIds);

    Optional<DogEquipment> findByUserDogIdAndCategory(Long userDogId, ItemCategory category);
}
