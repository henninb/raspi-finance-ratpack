package finance.services


import finance.domain.Payment
import finance.repositories.PaymentRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Timestamp

@Log
@CompileStatic
class PaymentService implements Service {

    private PaymentRepository paymentRepository

    @Inject
    PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository
    }

    List<Payment> payments() {
        return paymentRepository.payments()
    }

    Payment paymentInsert(Payment payment) {
        payment.dateUpdated = new Timestamp(System.currentTimeMillis())
        payment.dateAdded = new Timestamp(System.currentTimeMillis())
        payment.guidSource = UUID.randomUUID()
        payment.guidDestination = UUID.randomUUID()

        //TODO: insert transactions

        paymentRepository.paymentInsert(payment)
        return payment
    }
}
