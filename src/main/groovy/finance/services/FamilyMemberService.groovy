package finance.services

import finance.domain.FamilyMember
import finance.repositories.FamilyMemberRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Timestamp

@Log
@CompileStatic
class FamilyMemberService implements Service {

    private FamilyMemberRepository familyMemberRepository

    @Inject
    FamilyMemberService(FamilyMemberRepository familyMemberRepository) {
        this.familyMemberRepository = familyMemberRepository
    }

    List<FamilyMember> familyMembers() {
        return familyMemberRepository.familyMembers()
    }

    FamilyMember familyMember(Long familyMemberId) {
        return familyMemberRepository.familyMember(familyMemberId)
    }

    FamilyMember familyMemberInsert(FamilyMember familyMember) {
        familyMember.dateUpdated = new Timestamp(System.currentTimeMillis())
        familyMember.dateAdded = new Timestamp(System.currentTimeMillis())
        familyMemberRepository.familyMemberInsert(familyMember)
        log.info("inserted family member ${familyMember.memberName}")
        return familyMember
    }

    FamilyMember familyMemberUpdate(FamilyMember familyMember) {
        FamilyMember existing = familyMemberRepository.familyMember(familyMember.familyMemberId)
        if (!existing) {
            throw new RuntimeException("family member not found: ${familyMember.familyMemberId}")
        }
        familyMember.dateUpdated = new Timestamp(System.currentTimeMillis())
        familyMemberRepository.familyMemberUpdate(familyMember)
        return familyMemberRepository.familyMember(familyMember.familyMemberId)
    }

    boolean familyMemberDelete(Long familyMemberId) {
        FamilyMember existing = familyMemberRepository.familyMember(familyMemberId)
        if (!existing) {
            return false
        }
        return familyMemberRepository.familyMemberDelete(familyMemberId)
    }
}
