package finance.services

import finance.domain.Account
import finance.domain.ValidationAmount
import finance.repositories.AccountRepository
import finance.repositories.ValidationAmountRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Timestamp

@Slf4j
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
        if (accountNameOwner) {
            Account account = accountRepository.account(accountNameOwner)
            validationAmount.accountId = account.accountId
        }
        validationAmount.dateUpdated = new Timestamp(System.currentTimeMillis())
        validationAmount.dateAdded = new Timestamp(System.currentTimeMillis())
        validationAmountRepository.validationAmountInsert(validationAmount)
        try {
            accountRepository.updateValidationDateForAccount(validationAmount.accountId)
        } catch (Exception e) {
            log.warn("Failed to refresh account.validation_date for accountId=${validationAmount.accountId}: ${e.message}")
        }
        return validationAmount
    }

    ValidationAmount validationAmountUpdate(ValidationAmount validationAmount) {
        ValidationAmount existing = validationAmountRepository.validationAmount(validationAmount.validationId)
        if (!existing) {
            throw new RuntimeException("validation amount not found: ${validationAmount.validationId}")
        }
        validationAmountRepository.validationAmountUpdate(validationAmount)
        ValidationAmount updated = validationAmountRepository.validationAmount(validationAmount.validationId)
        try {
            accountRepository.updateValidationDateForAccount(updated.accountId)
        } catch (Exception e) {
            log.warn("Failed to refresh account.validation_date for accountId=${updated.accountId}: ${e.message}")
        }
        return updated
    }

    boolean validationAmountDelete(Long validationId) {
        ValidationAmount existing = validationAmountRepository.validationAmount(validationId)
        if (!existing) {
            return false
        }
        return validationAmountRepository.validationAmountDelete(validationId)
    }
}
