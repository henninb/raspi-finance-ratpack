package finance.service

import finance.domain.Parameter
import finance.repositories.ParameterRepository
import finance.services.ParameterService
import spock.lang.Specification

class ParameterServiceSpec extends Specification {

    ParameterRepository parameterRepository = Mock()
    ParameterService service = new ParameterService(parameterRepository)

    private Parameter buildParameter(String name = 'feature.flag') {
        return new Parameter(parameterName: name, parameterValue: 'true', activeStatus: true)
    }

    def 'parameters delegates to repository'() {
        given:
        parameterRepository.parameters() >> [buildParameter()]

        when:
        List<Parameter> result = service.parameters()

        then:
        result.size() == 1
    }

    def 'parameter delegates to repository'() {
        given:
        parameterRepository.parameter('feature.flag') >> buildParameter()

        when:
        Parameter result = service.parameter('feature.flag')

        then:
        result.parameterName == 'feature.flag'
    }

    def 'parameterInsert inserts and returns parameter'() {
        given:
        Parameter p = buildParameter()
        parameterRepository.parameter('feature.flag') >> null

        when:
        Parameter result = service.parameterInsert(p)

        then:
        1 * parameterRepository.parameterInsert(p)
        result == p
    }

    def 'parameterInsert throws when parameter already exists'() {
        given:
        parameterRepository.parameter('feature.flag') >> buildParameter()

        when:
        service.parameterInsert(buildParameter())

        then:
        thrown(RuntimeException)
        0 * parameterRepository.parameterInsert(_)
    }

    def 'parameterUpdate throws when parameter not found'() {
        given:
        parameterRepository.parameter('missing') >> null

        when:
        service.parameterUpdate(new Parameter(parameterName: 'missing'))

        then:
        thrown(RuntimeException)
    }

    def 'parameterUpdate delegates and returns refreshed parameter'() {
        given:
        Parameter existing = buildParameter()
        Parameter updated = buildParameter()
        updated.parameterValue = 'false'
        parameterRepository.parameter('feature.flag') >>> [existing, updated]

        when:
        Parameter result = service.parameterUpdate(buildParameter())

        then:
        1 * parameterRepository.parameterUpdate(_)
        result.parameterValue == 'false'
    }

    def 'parameterDelete returns false when parameter not found'() {
        given:
        parameterRepository.parameter('missing') >> null

        expect:
        !service.parameterDelete('missing')
    }

    def 'parameterDelete returns true when parameter exists'() {
        given:
        parameterRepository.parameter('feature.flag') >> buildParameter()
        parameterRepository.parameterDelete('feature.flag') >> true

        expect:
        service.parameterDelete('feature.flag')
    }
}
