package com.diving.pungdong.repo.lecture;

import com.diving.pungdong.domain.lecture.Lecture;
import com.diving.pungdong.dto.lecture.search.CostCondition;
import com.diving.pungdong.dto.lecture.search.SearchCondition;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import javax.persistence.EntityManager;
import java.util.List;

import static com.diving.pungdong.domain.lecture.QLecture.lecture;
import static com.diving.pungdong.domain.lecture.QLectureImage.lectureImage;

public class LectureJpaRepoImpl implements LectureJpaRepoCustom {
    private final JPAQueryFactory queryFactory;

    public LectureJpaRepoImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<Lecture> searchListByCondition(SearchCondition condition, Pageable pageable) {
        List<Lecture> content = queryFactory
                .selectFrom(lecture)
                .join(lecture.lectureImages, lectureImage).fetchJoin()
                .where(
                        groupNameEq(condition.getGroupName()),
                        certificateKindEq(condition.getCertificateKind()),
                        regionEq(condition.getRegion()),
                        costBetween(condition.getCostCondition()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(lecture)
                .join(lecture.lectureImages, lectureImage).fetchJoin()
                .where(
                        certificateKindEq(condition.getCertificateKind()),
                        regionEq(condition.getRegion()),
                        costBetween(condition.getCostCondition()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression groupNameEq(String groupName) {
        return groupName == null ? null : lecture.groupName.eq(groupName);
    }

    private BooleanExpression certificateKindEq(String certificateKind) {
        return certificateKind == null ? null : lecture.certificateKind.eq(certificateKind);
    }

    private BooleanExpression regionEq(String region) {
        return region == null ? null : lecture.region.eq(region);
    }

    private BooleanExpression costBetween(CostCondition costCondition) {
        return costCondition == null || costCondition.equals(new CostCondition())
                ? null : lecture.price.between(costCondition.getMin(), costCondition.getMax());
    }
}
