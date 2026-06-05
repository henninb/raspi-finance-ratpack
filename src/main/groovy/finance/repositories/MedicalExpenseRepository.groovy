package finance.repositories

import com.google.inject.Inject
import finance.domain.MedicalExpense
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import java.time.LocalDate

import static org.jooq.generated.Tables.T_MEDICAL_EXPENSE
import static org.jooq.generated.Tables.T_TRANSACTION

@Log
class MedicalExpenseRepository {
    private final DSLContext dslContext

    @Inject
    MedicalExpenseRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    List<MedicalExpense> medicalExpenses() {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true))
                .fetchInto(MedicalExpense)
    }

    MedicalExpense medicalExpense(Long medicalExpenseId) {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.MEDICAL_EXPENSE_ID.equal(medicalExpenseId))
                .fetchOneInto(MedicalExpense)
    }

    List<MedicalExpense> medicalExpensesByOwner(String owner) {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.OWNER.equal(owner).and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    boolean medicalExpenseInsert(MedicalExpense medicalExpense) {
        dslContext.newRecord(T_MEDICAL_EXPENSE, medicalExpense).store()
        return true
    }

    boolean medicalExpenseUpdate(MedicalExpense medicalExpense) {
        dslContext.update(T_MEDICAL_EXPENSE)
                .set(T_MEDICAL_EXPENSE.OWNER, medicalExpense.owner)
                .set(T_MEDICAL_EXPENSE.TRANSACTION_ID, medicalExpense.transactionId)
                .set(T_MEDICAL_EXPENSE.PROVIDER_ID, medicalExpense.providerId)
                .set(T_MEDICAL_EXPENSE.FAMILY_MEMBER_ID, medicalExpense.familyMemberId)
                .set(T_MEDICAL_EXPENSE.SERVICE_DATE, medicalExpense.serviceDate?.toLocalDate())
                .set(T_MEDICAL_EXPENSE.SERVICE_DESCRIPTION, medicalExpense.serviceDescription)
                .set(T_MEDICAL_EXPENSE.PROCEDURE_CODE, medicalExpense.procedureCode)
                .set(T_MEDICAL_EXPENSE.DIAGNOSIS_CODE, medicalExpense.diagnosisCode)
                .set(T_MEDICAL_EXPENSE.BILLED_AMOUNT, medicalExpense.billedAmount)
                .set(T_MEDICAL_EXPENSE.INSURANCE_DISCOUNT, medicalExpense.insuranceDiscount)
                .set(T_MEDICAL_EXPENSE.INSURANCE_PAID, medicalExpense.insurancePaid)
                .set(T_MEDICAL_EXPENSE.PATIENT_RESPONSIBILITY, medicalExpense.patientResponsibility)
                .set(T_MEDICAL_EXPENSE.PAID_DATE, medicalExpense.paidDate?.toLocalDate())
                .set(T_MEDICAL_EXPENSE.IS_OUT_OF_NETWORK, medicalExpense.isOutOfNetwork)
                .set(T_MEDICAL_EXPENSE.CLAIM_NUMBER, medicalExpense.claimNumber)
                .set(T_MEDICAL_EXPENSE.CLAIM_STATUS, medicalExpense.claimStatus)
                .set(T_MEDICAL_EXPENSE.ACTIVE_STATUS, medicalExpense.activeStatus)
                .set(T_MEDICAL_EXPENSE.PAID_AMOUNT, medicalExpense.paidAmount)
                .where(T_MEDICAL_EXPENSE.MEDICAL_EXPENSE_ID.eq(medicalExpense.medicalExpenseId))
                .execute()
        return true
    }

    boolean medicalExpenseDelete(Long medicalExpenseId) {
        dslContext.delete(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.MEDICAL_EXPENSE_ID.equal(medicalExpenseId))
                .execute()
        return true
    }

    MedicalExpense medicalExpenseByTransactionId(Long transactionId) {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.TRANSACTION_ID.eq(transactionId))
                .fetchOneInto(MedicalExpense)
    }

    List<MedicalExpense> medicalExpensesByAccountId(Long accountId) {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.TRANSACTION_ID.in(
                        dslContext.select(T_TRANSACTION.TRANSACTION_ID)
                                .from(T_TRANSACTION)
                                .where(T_TRANSACTION.ACCOUNT_ID.eq(accountId))
                ).and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    List<MedicalExpense> medicalExpensesByAccountIdAndDateRange(Long accountId, LocalDate startDate, LocalDate endDate) {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.TRANSACTION_ID.in(
                        dslContext.select(T_TRANSACTION.TRANSACTION_ID)
                                .from(T_TRANSACTION)
                                .where(T_TRANSACTION.ACCOUNT_ID.eq(accountId))
                ).and(T_MEDICAL_EXPENSE.SERVICE_DATE.between(startDate, endDate))
                 .and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    List<MedicalExpense> medicalExpensesByProviderId(Long providerId) {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.PROVIDER_ID.eq(providerId).and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    List<MedicalExpense> medicalExpensesByFamilyMemberId(Long familyMemberId) {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.FAMILY_MEMBER_ID.eq(familyMemberId).and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    List<MedicalExpense> medicalExpensesByFamilyMemberIdAndDateRange(Long familyMemberId, LocalDate startDate, LocalDate endDate) {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.FAMILY_MEMBER_ID.eq(familyMemberId)
                        .and(T_MEDICAL_EXPENSE.SERVICE_DATE.between(startDate, endDate))
                        .and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    List<MedicalExpense> medicalExpensesByClaimStatus(String claimStatus) {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.CLAIM_STATUS.eq(claimStatus).and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    List<MedicalExpense> outOfNetworkExpenses() {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.IS_OUT_OF_NETWORK.eq(true).and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    List<MedicalExpense> outstandingPatientBalances() {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.PATIENT_RESPONSIBILITY.gt(T_MEDICAL_EXPENSE.PAID_AMOUNT)
                        .and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    List<MedicalExpense> openClaims() {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.CLAIM_STATUS.in('submitted', 'pending')
                        .and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    List<MedicalExpense> medicalExpensesByProcedureCode(String procedureCode) {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.PROCEDURE_CODE.eq(procedureCode).and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    List<MedicalExpense> medicalExpensesByDiagnosisCode(String diagnosisCode) {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.DIAGNOSIS_CODE.eq(diagnosisCode).and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    List<MedicalExpense> medicalExpensesByDateRange(LocalDate startDate, LocalDate endDate) {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.SERVICE_DATE.between(startDate, endDate)
                        .and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    List<MedicalExpense> unpaidMedicalExpenses() {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.PAID_AMOUNT.eq(BigDecimal.ZERO)
                        .and(T_MEDICAL_EXPENSE.PATIENT_RESPONSIBILITY.gt(BigDecimal.ZERO))
                        .and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    List<MedicalExpense> partiallyPaidMedicalExpenses() {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.PAID_AMOUNT.gt(BigDecimal.ZERO)
                        .and(T_MEDICAL_EXPENSE.PAID_AMOUNT.lt(T_MEDICAL_EXPENSE.PATIENT_RESPONSIBILITY))
                        .and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    List<MedicalExpense> fullyPaidMedicalExpenses() {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.PAID_AMOUNT.ge(T_MEDICAL_EXPENSE.PATIENT_RESPONSIBILITY)
                        .and(T_MEDICAL_EXPENSE.PATIENT_RESPONSIBILITY.gt(BigDecimal.ZERO))
                        .and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    List<MedicalExpense> medicalExpensesWithoutTransaction() {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.TRANSACTION_ID.isNull()
                        .and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    List<MedicalExpense> overpaidMedicalExpenses() {
        return dslContext.selectFrom(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.PAID_AMOUNT.gt(T_MEDICAL_EXPENSE.PATIENT_RESPONSIBILITY)
                        .and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .orderBy(T_MEDICAL_EXPENSE.SERVICE_DATE.desc())
                .fetchInto(MedicalExpense)
    }

    boolean medicalExpenseUpdateClaimStatus(Long medicalExpenseId, String claimStatus) {
        dslContext.update(T_MEDICAL_EXPENSE)
                .set(T_MEDICAL_EXPENSE.CLAIM_STATUS, claimStatus)
                .where(T_MEDICAL_EXPENSE.MEDICAL_EXPENSE_ID.eq(medicalExpenseId))
                .execute()
        return true
    }

    boolean medicalExpenseLinkTransaction(Long medicalExpenseId, Long transactionId) {
        dslContext.update(T_MEDICAL_EXPENSE)
                .set(T_MEDICAL_EXPENSE.TRANSACTION_ID, transactionId)
                .where(T_MEDICAL_EXPENSE.MEDICAL_EXPENSE_ID.eq(medicalExpenseId))
                .execute()
        return true
    }

    boolean medicalExpenseUnlinkTransaction(Long medicalExpenseId) {
        dslContext.update(T_MEDICAL_EXPENSE)
                .setNull(T_MEDICAL_EXPENSE.TRANSACTION_ID)
                .where(T_MEDICAL_EXPENSE.MEDICAL_EXPENSE_ID.eq(medicalExpenseId))
                .execute()
        return true
    }

    Map<String, Long> claimStatusCounts() {
        return dslContext.select(T_MEDICAL_EXPENSE.CLAIM_STATUS, DSL.count())
                .from(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true))
                .groupBy(T_MEDICAL_EXPENSE.CLAIM_STATUS)
                .fetch()
                .collectEntries { [(it.value1()): (it.value2() as Long)] }
    }

    Map<String, BigDecimal> totalsByYear(int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1)
        LocalDate yearEnd = LocalDate.of(year, 12, 31)
        def record = dslContext.select(
                DSL.coalesce(DSL.sum(T_MEDICAL_EXPENSE.BILLED_AMOUNT), BigDecimal.ZERO),
                DSL.coalesce(DSL.sum(T_MEDICAL_EXPENSE.INSURANCE_PAID), BigDecimal.ZERO),
                DSL.coalesce(DSL.sum(T_MEDICAL_EXPENSE.PATIENT_RESPONSIBILITY), BigDecimal.ZERO),
                DSL.coalesce(DSL.sum(T_MEDICAL_EXPENSE.PAID_AMOUNT), BigDecimal.ZERO)
        )
                .from(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.SERVICE_DATE.between(yearStart, yearEnd)
                        .and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .fetchOne()
        if (!record) {
            return [billedAmount: BigDecimal.ZERO, insurancePaid: BigDecimal.ZERO, patientResponsibility: BigDecimal.ZERO, paidAmount: BigDecimal.ZERO]
        }
        return [
            billedAmount         : (record.value1() as BigDecimal) ?: BigDecimal.ZERO,
            insurancePaid        : (record.value2() as BigDecimal) ?: BigDecimal.ZERO,
            patientResponsibility: (record.value3() as BigDecimal) ?: BigDecimal.ZERO,
            paidAmount           : (record.value4() as BigDecimal) ?: BigDecimal.ZERO
        ]
    }

    BigDecimal totalPaidByYear(int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1)
        LocalDate yearEnd = LocalDate.of(year, 12, 31)
        return dslContext.select(DSL.coalesce(DSL.sum(T_MEDICAL_EXPENSE.PAID_AMOUNT), BigDecimal.ZERO))
                .from(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.SERVICE_DATE.between(yearStart, yearEnd)
                        .and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .fetchOneInto(BigDecimal) ?: BigDecimal.ZERO
    }

    BigDecimal totalUnpaidBalance() {
        return dslContext.select(
                DSL.coalesce(DSL.sum(T_MEDICAL_EXPENSE.PATIENT_RESPONSIBILITY.minus(T_MEDICAL_EXPENSE.PAID_AMOUNT)), BigDecimal.ZERO)
        )
                .from(T_MEDICAL_EXPENSE)
                .where(T_MEDICAL_EXPENSE.PATIENT_RESPONSIBILITY.gt(T_MEDICAL_EXPENSE.PAID_AMOUNT)
                        .and(T_MEDICAL_EXPENSE.ACTIVE_STATUS.eq(true)))
                .fetchOneInto(BigDecimal) ?: BigDecimal.ZERO
    }
}
