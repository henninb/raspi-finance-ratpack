package finance.services

import finance.domain.Account
import finance.domain.BonusProgress
import finance.domain.Category
import finance.domain.Description
import finance.domain.Transaction
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.DescriptionRepository
import finance.repositories.PaymentRepository
import finance.repositories.TransactionRepository
import finance.repositories.TransferRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Timestamp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Slf4j
@CompileStatic
class TransactionService implements Service {

    private TransactionRepository transactionRepository
    private AccountRepository accountRepository
    private CategoryRepository categoryRepository
    private DescriptionRepository descriptionRepository
    private PaymentRepository paymentRepository
    private TransferRepository transferRepository

    @Inject
    TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository, CategoryRepository categoryRepository, DescriptionRepository descriptionRepository, PaymentRepository paymentRepository, TransferRepository transferRepository) {
        this.transactionRepository = transactionRepository
        this.accountRepository = accountRepository
        this.categoryRepository = categoryRepository
        this.descriptionRepository = descriptionRepository
        this.paymentRepository = paymentRepository
        this.transferRepository = transferRepository
    }

    List<Transaction> transactionsAll() {
        return transactionRepository.transactionsAll()
    }

    List<Transaction> transactions(String accountNameOwner) {
        return transactionRepository.transactions(accountNameOwner)
    }

    List<Transaction> transactionsByCategory(String categoryName) {
        return transactionRepository.transactionsByCategory(categoryName)
    }

    List<Transaction> transactionsByDescription(String descriptionName) {
        return transactionRepository.transactionsByDescription(descriptionName)
    }

    Transaction transaction(String guid) {
        return transactionRepository.transaction(guid)
    }

    boolean deleteTransaction(String guid) {
        Transaction transaction = transactionRepository.transaction(guid)
        if (!transaction) {
            return false
        }
        if (paymentRepository.existsByTransactionGuid(guid)) {
            throw new RuntimeException("Cannot delete transaction ${guid} because it is referenced by a payment. Please delete the related payment first.")
        }
        if (transferRepository.existsByTransactionGuid(guid)) {
            throw new RuntimeException("Cannot delete transaction ${guid} because it is referenced by a transfer. Please delete the related transfer first.")
        }
        boolean deleted = transactionRepository.transactionDelete(guid)
        accountRepository.updateTotalsForAllAccounts()
        return deleted
    }

    // Internal cascade delete — bypasses FK reference guards, used by transfer/payment cascade deletes
    boolean deleteTransactionCascade(String guid) {
        if (!guid) {
            return false
        }
        Transaction transaction = transactionRepository.transaction(guid)
        if (!transaction) {
            log.warn("Transaction not found for cascade delete (stale reference): ${guid}")
            return false
        }
        return transactionRepository.transactionDelete(guid)
    }

    Transaction transactionInsert(Transaction transaction) {
        transaction.dateUpdated = new Timestamp(System.currentTimeMillis())
        transaction.dateAdded = new Timestamp(System.currentTimeMillis())

        Category category = categoryRepository.category(transaction.owner, transaction.category)
        if (!category) {
            categoryRepository.categoryInsert(
                    new Category(categoryName: transaction.category, owner: transaction.owner, activeStatus: true)
            )
        }
        if (transaction.notes) {
            transaction.notes = transaction.notes.toLowerCase()
        }
        if (transaction.description) {
            transaction.description = transaction.description.toLowerCase()
            Description description = descriptionRepository.description(transaction.owner, transaction.description)
            if (!description) {
                descriptionRepository.descriptionInsert(
                        new Description(descriptionName: transaction.description, owner: transaction.owner, activeStatus: true)
                )
            }
        }
        Account account = accountRepository.account(transaction.accountNameOwner)
        if (account) {
            transaction.accountType = account.accountType
            transaction.accountId = account.accountId
            transactionRepository.transactionInsert(transaction)
            accountRepository.updateTotalsForAllAccounts()
            log.info("inserted transaction ${transaction.guid}")
            return transaction
        }
        throw new RuntimeException("no account found for transaction ${transaction.guid}")
    }

    boolean transactionUpdate(Transaction transaction) {
        Transaction existing = transactionRepository.transaction(transaction.guid)
        if (!existing) {
            throw new RuntimeException("transaction not found: ${transaction.guid}")
        }
        String owner = existing.owner
        if (transaction.category) {
            Category category = categoryRepository.category(owner, transaction.category)
            if (!category) {
                categoryRepository.categoryInsert(new Category(categoryName: transaction.category, owner: owner, activeStatus: true))
            }
        }
        if (transaction.notes) {
            transaction.notes = transaction.notes.toLowerCase()
        }
        if (transaction.description) {
            transaction.description = transaction.description.toLowerCase()
            Description description = descriptionRepository.description(owner, transaction.description)
            if (!description) {
                descriptionRepository.descriptionInsert(new Description(descriptionName: transaction.description, owner: owner, activeStatus: true))
            }
        }
        boolean updated = transactionRepository.transactionUpdate(transaction)
        accountRepository.updateTotalsForAllAccounts()
        return updated
    }

    boolean transactionStateUpdate(String guid, String transactionState) {
        boolean result = transactionRepository.transactionStateUpdate(guid, transactionState)
        accountRepository.updateTotalsForAllAccounts()
        return result
    }

    List<Transaction> transactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("startDate must be before or equal to endDate")
        }
        return transactionRepository.transactionsByDateRange(startDate, endDate)
    }

    Transaction changeAccountNameOwner(String guid, String newAccountNameOwner) {
        Account account = accountRepository.account(newAccountNameOwner)
        if (!account) {
            throw new RuntimeException("account not found: ${newAccountNameOwner}")
        }
        Transaction transaction = transactionRepository.transaction(guid)
        if (!transaction) {
            throw new RuntimeException("transaction not found: ${guid}")
        }
        transactionRepository.changeAccountNameOwner(guid, newAccountNameOwner, account.accountId)
        log.info("changed accountNameOwner for transaction ${guid} to ${newAccountNameOwner}")
        return transactionRepository.transaction(guid)
    }

    BonusProgress calculateBonusProgress(String accountNameOwner, LocalDate startDate, BigDecimal targetAmount, BigDecimal bonusAmount, long windowDays) {
        LocalDate windowEndDate = startDate.plusDays(windowDays - 1)
        LocalDate today = LocalDate.now()
        BigDecimal spent = transactionRepository.sumSpendingInWindow(accountNameOwner, startDate, windowEndDate, "cleared")
        BigDecimal spentPending = transactionRepository.sumPendingSpendingInWindow(accountNameOwner, startDate, windowEndDate, ["outstanding", "future"])
        BigDecimal remaining = (targetAmount - spent).max(BigDecimal.ZERO)
        double rawPercent = targetAmount > BigDecimal.ZERO ? Math.min(spent.doubleValue() / targetAmount.doubleValue() * 100.0d, 100.0d) : 0.0d
        double percentComplete = (double) Math.round(rawPercent * 10.0d) / 10.0d
        long daysRemaining = today.isAfter(windowEndDate) ? 0L : ChronoUnit.DAYS.between(today, windowEndDate)
        return new BonusProgress(
                accountNameOwner: accountNameOwner,
                spent: spent,
                spentPending: spentPending,
                target: targetAmount,
                remaining: remaining,
                percentComplete: percentComplete,
                bonusAmount: bonusAmount,
                bonusEarned: spent >= targetAmount,
                windowStartDate: startDate,
                windowEndDate: windowEndDate,
                daysRemaining: daysRemaining
        )
    }
}
