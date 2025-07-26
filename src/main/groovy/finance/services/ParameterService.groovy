package finance.services

import finance.domain.Parameter
import finance.domain.Payment
import finance.repositories.ParameterRepository
import finance.repositories.PaymentRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import ratpack.core.service.Service

import javax.inject.Inject

@Log
@CompileStatic
class ParameterService implements Service {

    private ParameterRepository parameterRepository

    @Inject
    ParameterService(ParameterRepository parameterRepository) {
        this.parameterRepository = parameterRepository
    }

    Parameter parameter(String parameterName) {
        return parameterRepository.parameter(parameterName)
    }
}
