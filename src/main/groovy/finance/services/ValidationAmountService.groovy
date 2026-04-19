package finance.services

import finance.domain.Account
import finance.domain.ValidationAmount
import finance.repositories.AccountRepository
import finance.repositories.ValidationAmountRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Timestamp

@Log
@CompileStatic
class ValidationAmountService implements Service {

    private ValidationAmountRepository validationAmountRepository
    private AccountRepository accountRepository

    @Inject
    ValidationAmountService(ValidationAmountRepository validationAmountRepository, AccountRepository accountRepository) {
        this.validationAmountRepository = validationAmountRepository
        this.accountRepository = accountRepository
    }

    List<ValidationAmount> validationAmounts() {
        return validationAmountRepository.validationAmounts()
    }

    ValidationAmount validationAmount(String accountNameOwner) {
        Account account = accountRepository.account(accountNameOwner)
        List<ValidationAmount> validationAmounts = validationAmountRepository.validationAmounts(account.accountId)
        if (validationAmounts) {
            return validationAmounts.sort { it.validationDate }.last()
        }
        return new ValidationAmount()
    }

    ValidationAmount validationAmountById(Long validationId) {
        return validationAmountRepository.validationAmount(validationId)
    }

    List<ValidationAmount> validationAmountsByAccountAndState(String accountNameOwner, String transactionState) {
        return validationAmountRepository.validationAmountsByAccountAndState(accountNameOwner, transactionState)
    }

    ValidationAmount validationAmountInsert(String accountNameOwner, ValidationAmount validationAmount) {
        Account account = accountRepository.account(accountNameOwner)
        validationAmount.dateUpdated = new Timestamp(System.currentTimeMillis())
        validationAmount.dateAdded = new Timestamp(System.currentTimeMillis())
        validationAmount.accountId = account.accountId
        validationAmountRepository.validationAmountInsert(validationAmount)
        return validationAmount
    }

    ValidationAmount validationAmountUpdate(ValidationAmount validationAmount) {
        ValidationAmount existing = validationAmountRepository.validationAmount(validationAmount.validationId)
        if (!existing) {
            throw new RuntimeException("validation amount not found: ${validationAmount.validationId}")
        }
        validationAmountRepository.validationAmountUpdate(validationAmount)
        return validationAmountRepository.validationAmount(validationAmount.validationId)
    }

    boolean validationAmountDelete(Long validationId) {
        ValidationAmount existing = validationAmountRepository.validationAmount(validationId)
        if (!existing) {
            return false
        }
        return validationAmountRepository.validationAmountDelete(validationId)
    }
}
