package finance.services

import finance.domain.MedicalExpense
import finance.repositories.MedicalExpenseRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Timestamp
import java.time.LocalDate

@Log
@CompileStatic
class MedicalExpenseService implements Service {

    private MedicalExpenseRepository medicalExpenseRepository

    @Inject
    MedicalExpenseService(MedicalExpenseRepository medicalExpenseRepository) {
        this.medicalExpenseRepository = medicalExpenseRepository
    }

    List<MedicalExpense> medicalExpenses() {
        return medicalExpenseRepository.medicalExpenses()
    }

    MedicalExpense medicalExpense(Long medicalExpenseId) {
        return medicalExpenseRepository.medicalExpense(medicalExpenseId)
    }

    List<MedicalExpense> medicalExpensesByOwner(String owner) {
        return medicalExpenseRepository.medicalExpensesByOwner(owner)
    }

    MedicalExpense medicalExpenseInsert(MedicalExpense medicalExpense) {
        medicalExpense.dateUpdated = new Timestamp(System.currentTimeMillis())
        medicalExpense.dateAdded = new Timestamp(System.currentTimeMillis())
        medicalExpenseRepository.medicalExpenseInsert(medicalExpense)
        log.info("inserted medical expense for owner ${medicalExpense.owner}")
        return medicalExpense
    }

    MedicalExpense medicalExpenseUpdate(MedicalExpense medicalExpense) {
        MedicalExpense existing = medicalExpenseRepository.medicalExpense(medicalExpense.medicalExpenseId)
        if (!existing) {
            throw new RuntimeException("medical expense not found: ${medicalExpense.medicalExpenseId}")
        }
        medicalExpense.dateUpdated = new Timestamp(System.currentTimeMillis())
        medicalExpenseRepository.medicalExpenseUpdate(medicalExpense)
        return medicalExpenseRepository.medicalExpense(medicalExpense.medicalExpenseId)
    }

    boolean medicalExpenseDelete(Long medicalExpenseId) {
        MedicalExpense existing = medicalExpenseRepository.medicalExpense(medicalExpenseId)
        if (!existing) {
            return false
        }
        return medicalExpenseRepository.medicalExpenseDelete(medicalExpenseId)
    }

    MedicalExpense medicalExpenseByTransactionId(Long transactionId) {
        return medicalExpenseRepository.medicalExpenseByTransactionId(transactionId)
    }

    List<MedicalExpense> medicalExpensesByAccountId(Long accountId) {
        return medicalExpenseRepository.medicalExpensesByAccountId(accountId)
    }

    List<MedicalExpense> medicalExpensesByAccountIdAndDateRange(Long accountId, LocalDate startDate, LocalDate endDate) {
        return medicalExpenseRepository.medicalExpensesByAccountIdAndDateRange(accountId, startDate, endDate)
    }

    List<MedicalExpense> medicalExpensesByProviderId(Long providerId) {
        return medicalExpenseRepository.medicalExpensesByProviderId(providerId)
    }

    List<MedicalExpense> medicalExpensesByFamilyMemberId(Long familyMemberId) {
        return medicalExpenseRepository.medicalExpensesByFamilyMemberId(familyMemberId)
    }

    List<MedicalExpense> medicalExpensesByFamilyMemberIdAndDateRange(Long familyMemberId, LocalDate startDate, LocalDate endDate) {
        return medicalExpenseRepository.medicalExpensesByFamilyMemberIdAndDateRange(familyMemberId, startDate, endDate)
    }

    List<MedicalExpense> medicalExpensesByClaimStatus(String claimStatus) {
        return medicalExpenseRepository.medicalExpensesByClaimStatus(claimStatus)
    }

    List<MedicalExpense> outOfNetworkExpenses() {
        return medicalExpenseRepository.outOfNetworkExpenses()
    }

    List<MedicalExpense> outstandingPatientBalances() {
        return medicalExpenseRepository.outstandingPatientBalances()
    }

    List<MedicalExpense> openClaims() {
        return medicalExpenseRepository.openClaims()
    }

    List<MedicalExpense> medicalExpensesByProcedureCode(String procedureCode) {
        return medicalExpenseRepository.medicalExpensesByProcedureCode(procedureCode)
    }

    List<MedicalExpense> medicalExpensesByDiagnosisCode(String diagnosisCode) {
        return medicalExpenseRepository.medicalExpensesByDiagnosisCode(diagnosisCode)
    }

    List<MedicalExpense> medicalExpensesByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("startDate must be before or equal to endDate")
        }
        return medicalExpenseRepository.medicalExpensesByDateRange(startDate, endDate)
    }

    List<MedicalExpense> unpaidMedicalExpenses() {
        return medicalExpenseRepository.unpaidMedicalExpenses()
    }

    List<MedicalExpense> partiallyPaidMedicalExpenses() {
        return medicalExpenseRepository.partiallyPaidMedicalExpenses()
    }

    List<MedicalExpense> fullyPaidMedicalExpenses() {
        return medicalExpenseRepository.fullyPaidMedicalExpenses()
    }

    List<MedicalExpense> medicalExpensesWithoutTransaction() {
        return medicalExpenseRepository.medicalExpensesWithoutTransaction()
    }

    List<MedicalExpense> overpaidMedicalExpenses() {
        return medicalExpenseRepository.overpaidMedicalExpenses()
    }

    MedicalExpense medicalExpenseUpdateClaimStatus(Long medicalExpenseId, String claimStatus) {
        MedicalExpense existing = medicalExpenseRepository.medicalExpense(medicalExpenseId)
        if (!existing) {
            throw new RuntimeException("medical expense not found: ${medicalExpenseId}")
        }
        medicalExpenseRepository.medicalExpenseUpdateClaimStatus(medicalExpenseId, claimStatus)
        return medicalExpenseRepository.medicalExpense(medicalExpenseId)
    }

    MedicalExpense medicalExpenseLinkTransaction(Long medicalExpenseId, Long transactionId) {
        MedicalExpense existing = medicalExpenseRepository.medicalExpense(medicalExpenseId)
        if (!existing) {
            throw new RuntimeException("medical expense not found: ${medicalExpenseId}")
        }
        medicalExpenseRepository.medicalExpenseLinkTransaction(medicalExpenseId, transactionId)
        return medicalExpenseRepository.medicalExpense(medicalExpenseId)
    }

    MedicalExpense medicalExpenseUnlinkTransaction(Long medicalExpenseId) {
        MedicalExpense existing = medicalExpenseRepository.medicalExpense(medicalExpenseId)
        if (!existing) {
            throw new RuntimeException("medical expense not found: ${medicalExpenseId}")
        }
        medicalExpenseRepository.medicalExpenseUnlinkTransaction(medicalExpenseId)
        return medicalExpenseRepository.medicalExpense(medicalExpenseId)
    }

    Map<String, Long> claimStatusCounts() {
        return medicalExpenseRepository.claimStatusCounts()
    }

    Map<String, BigDecimal> medicalExpenseTotalsByYear(int year) {
        return medicalExpenseRepository.totalsByYear(year)
    }

    BigDecimal totalPaidByYear(int year) {
        return medicalExpenseRepository.totalPaidByYear(year)
    }

    BigDecimal totalUnpaidBalance() {
        return medicalExpenseRepository.totalUnpaidBalance()
    }
}
