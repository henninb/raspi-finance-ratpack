package finance.services

import finance.domain.Summary
import finance.repositories.SummaryRepository
import ratpack.core.service.Service

import javax.inject.Inject

class SummaryService implements Service {

    private SummaryRepository summaryRepository

    @Inject
    PaymentService(SummaryRepository summaryRepository) {
        this.summaryRepository = summaryRepository
    }

    Summary summary(String accountNameOwner) {
        return summaryRepository.summary(accountNameOwner)
    }

    Summary summaryAll() {
        return summaryRepository.summaryAll()
    }
}
