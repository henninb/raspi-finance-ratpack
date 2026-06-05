package finance.repositories

import com.google.inject.Inject
import finance.domain.MedicalExpense
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import static org.jooq.generated.Tables.T_MEDICAL_EXPENSE

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
}
