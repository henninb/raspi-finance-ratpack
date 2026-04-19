package finance.services

import finance.domain.Parameter
import finance.repositories.ParameterRepository
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

    List<Parameter> parameters() {
        return parameterRepository.parameters()
    }

    Parameter parameter(String parameterName) {
        return parameterRepository.parameter(parameterName)
    }

    Parameter parameterInsert(Parameter parameter) {
        if (parameterRepository.parameter(parameter.parameterName)) {
            throw new RuntimeException("parameter already exists: ${parameter.parameterName}")
        }
        parameterRepository.parameterInsert(parameter)
        log.info("inserted parameter ${parameter.parameterName}")
        return parameter
    }

    Parameter parameterUpdate(Parameter parameter) {
        Parameter existing = parameterRepository.parameter(parameter.parameterName)
        if (!existing) {
            throw new RuntimeException("parameter not found: ${parameter.parameterName}")
        }
        parameterRepository.parameterUpdate(parameter)
        return parameterRepository.parameter(parameter.parameterName)
    }

    boolean parameterDelete(String parameterName) {
        Parameter existing = parameterRepository.parameter(parameterName)
        if (!existing) {
            return false
        }
        return parameterRepository.parameterDelete(parameterName)
    }
}
