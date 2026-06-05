package finance.domain

import groovy.transform.ToString

import java.time.LocalDate

@ToString
class BonusProgress {
    String accountNameOwner
    BigDecimal spent
    BigDecimal spentPending
    BigDecimal target
    BigDecimal remaining
    double percentComplete
    BigDecimal bonusAmount
    boolean bonusEarned
    LocalDate windowStartDate
    LocalDate windowEndDate
    long daysRemaining
}
