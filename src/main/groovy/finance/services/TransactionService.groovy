package finance.services

import finance.domain.Account
import finance.domain.BonusProgress
import finance.domain.Category
import finance.domain.Transaction
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.TransactionRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Timestamp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Log
@CompileStatic
class TransactionService implements Service {

    private TransactionRepository transactionRepository
    private AccountRepository accountRepository
    private CategoryRepository categoryRepository

    @Inject
    TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository
        this.accountRepository = accountRepository
        this.categoryRepository = categoryRepository
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
        if (transaction) {
            return transactionRepository.transactionDelete(guid)
        }
        return false
    }

    Transaction transactionInsert(Transaction transaction) {
        transaction.dateUpdated = new Timestamp(System.currentTimeMillis())
        transaction.dateAdded = new Timestamp(System.currentTimeMillis())

        Category category = categoryRepository.category(transaction.category)
        if (!category) {
            categoryRepository.categoryInsert(
                    new Category(categoryName: transaction.category, activeStatus: true)
            )
        }
        Account account = accountRepository.account(transaction.accountNameOwner)
        if (account) {
            transaction.accountType = account.accountType
            transaction.accountId = account.accountId
            transactionRepository.transactionInsert(transaction)
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
        return transactionRepository.transactionUpdate(transaction)
    }

    boolean transactionStateUpdate(String guid, String transactionState) {
        return transactionRepository.transactionStateUpdate(guid, transactionState)
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
