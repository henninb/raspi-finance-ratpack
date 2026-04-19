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

    Payment payment(Long paymentId) {
        return paymentRepository.payment(paymentId)
    }

    Payment paymentInsert(Payment payment) {
        payment.dateUpdated = new Timestamp(System.currentTimeMillis())
        payment.dateAdded = new Timestamp(System.currentTimeMillis())
        payment.guidSource = UUID.randomUUID().toString()
        payment.guidDestination = UUID.randomUUID().toString()
        paymentRepository.paymentInsert(payment)
        return payment
    }

    Payment paymentUpdate(Payment payment) {
        Payment existing = paymentRepository.payment(payment.paymentId)
        if (!existing) {
            throw new RuntimeException("payment not found: ${payment.paymentId}")
        }
        paymentRepository.paymentUpdate(payment)
        return paymentRepository.payment(payment.paymentId)
    }

    boolean paymentDelete(Long paymentId) {
        Payment existing = paymentRepository.payment(paymentId)
        if (!existing) {
            return false
        }
        return paymentRepository.paymentDelete(paymentId)
    }
}
