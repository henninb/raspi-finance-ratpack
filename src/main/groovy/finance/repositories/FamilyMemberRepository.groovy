package finance.repositories

import com.google.inject.Inject
import finance.domain.FamilyMember
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import static org.jooq.generated.Tables.T_FAMILY_MEMBER

@Log
class FamilyMemberRepository {
    private final DSLContext dslContext

    @Inject
    FamilyMemberRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    List<FamilyMember> familyMembers() {
        return dslContext.selectFrom(T_FAMILY_MEMBER)
                .where(T_FAMILY_MEMBER.ACTIVE_STATUS.eq(true))
                .fetchInto(FamilyMember)
    }

    FamilyMember familyMember(Long familyMemberId) {
        return dslContext.selectFrom(T_FAMILY_MEMBER)
                .where(T_FAMILY_MEMBER.FAMILY_MEMBER_ID.equal(familyMemberId))
                .fetchOneInto(FamilyMember)
    }

    boolean familyMemberInsert(FamilyMember familyMember) {
        dslContext.insertInto(T_FAMILY_MEMBER)
                .set(T_FAMILY_MEMBER.OWNER, familyMember.owner ?: "")
                .set(T_FAMILY_MEMBER.MEMBER_NAME, (String) familyMember.memberName)
                .set(T_FAMILY_MEMBER.RELATIONSHIP, (String) familyMember.relationship)
                .set(T_FAMILY_MEMBER.DATE_OF_BIRTH, (java.time.LocalDate) familyMember.dateOfBirth?.toLocalDate())
                .set(T_FAMILY_MEMBER.INSURANCE_MEMBER_ID, (String) familyMember.insuranceMemberId)
                .set(T_FAMILY_MEMBER.SSN_LAST_FOUR, (String) familyMember.ssnLastFour)
                .set(T_FAMILY_MEMBER.MEDICAL_RECORD_NUMBER, (String) familyMember.medicalRecordNumber)
                .set(T_FAMILY_MEMBER.ACTIVE_STATUS, (Boolean) familyMember.activeStatus)
                .execute()
        return true
    }

    boolean familyMemberUpdate(FamilyMember familyMember) {
        dslContext.update(T_FAMILY_MEMBER)
                .set(T_FAMILY_MEMBER.OWNER, familyMember.owner)
                .set(T_FAMILY_MEMBER.MEMBER_NAME, familyMember.memberName)
                .set(T_FAMILY_MEMBER.RELATIONSHIP, familyMember.relationship)
                .set(T_FAMILY_MEMBER.DATE_OF_BIRTH, familyMember.dateOfBirth?.toLocalDate())
                .set(T_FAMILY_MEMBER.INSURANCE_MEMBER_ID, familyMember.insuranceMemberId)
                .set(T_FAMILY_MEMBER.SSN_LAST_FOUR, familyMember.ssnLastFour)
                .set(T_FAMILY_MEMBER.MEDICAL_RECORD_NUMBER, familyMember.medicalRecordNumber)
                .set(T_FAMILY_MEMBER.ACTIVE_STATUS, familyMember.activeStatus)
                .where(T_FAMILY_MEMBER.FAMILY_MEMBER_ID.eq(familyMember.familyMemberId))
                .execute()
        return true
    }

    boolean familyMemberDelete(Long familyMemberId) {
        dslContext.delete(T_FAMILY_MEMBER)
                .where(T_FAMILY_MEMBER.FAMILY_MEMBER_ID.equal(familyMemberId))
                .execute()
        return true
    }

    List<FamilyMember> familyMembersByOwner(String owner) {
        return dslContext.selectFrom(T_FAMILY_MEMBER)
                .where(T_FAMILY_MEMBER.OWNER.eq(owner).and(T_FAMILY_MEMBER.ACTIVE_STATUS.eq(true)))
                .orderBy(T_FAMILY_MEMBER.MEMBER_NAME)
                .fetchInto(FamilyMember)
    }

    List<FamilyMember> familyMembersByOwnerAndRelationship(String owner, String relationship) {
        return dslContext.selectFrom(T_FAMILY_MEMBER)
                .where(T_FAMILY_MEMBER.OWNER.eq(owner)
                        .and(T_FAMILY_MEMBER.RELATIONSHIP.eq(relationship))
                        .and(T_FAMILY_MEMBER.ACTIVE_STATUS.eq(true)))
                .orderBy(T_FAMILY_MEMBER.MEMBER_NAME)
                .fetchInto(FamilyMember)
    }

    boolean familyMemberActivate(Long familyMemberId) {
        dslContext.update(T_FAMILY_MEMBER)
                .set(T_FAMILY_MEMBER.ACTIVE_STATUS, true)
                .where(T_FAMILY_MEMBER.FAMILY_MEMBER_ID.eq(familyMemberId))
                .execute()
        return true
    }

    boolean familyMemberDeactivate(Long familyMemberId) {
        dslContext.update(T_FAMILY_MEMBER)
                .set(T_FAMILY_MEMBER.ACTIVE_STATUS, false)
                .where(T_FAMILY_MEMBER.FAMILY_MEMBER_ID.eq(familyMemberId))
                .execute()
        return true
    }
}
