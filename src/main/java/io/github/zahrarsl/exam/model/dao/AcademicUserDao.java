package io.github.zahrarsl.exam.model.dao;

import io.github.zahrarsl.exam.model.entity.AcademicUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.List;

@org.springframework.stereotype.Repository
public interface AcademicUserDao extends Repository<AcademicUser, Integer> , JpaSpecificationExecutor<AcademicUser> {
    AcademicUser save(AcademicUser academicUser);
    AcademicUser findById(int id);
    void deleteById(int id);

    List<AcademicUser> findByAdminVerificationStatusIsFalseAndEmailVerificationStatusIsTrue(Pageable pageable);
    List<AcademicUser> findByAdminVerificationStatusIsTrueAndRoleEquals(String role);
    List<AcademicUser> findByAdminVerificationStatusIsTrue();

    @Modifying
    @Transactional
    @Query("update User u set u.adminVerificationStatus=true where u.id =:id")
    void setAdminVerificationStatusTrue(@Param("id") int id);

    @Modifying
    @Transactional
    @Query("update User u set u.adminVerificationStatus=true where u.id in (:idList)")
    void setAdminVerificationStatusTrueByList(@Param("idList") List idList);
}