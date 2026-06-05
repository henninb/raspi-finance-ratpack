package finance.services

import finance.domain.MedicalExpense
import finance.repositories.MedicalExpenseRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Timestamp

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
}
