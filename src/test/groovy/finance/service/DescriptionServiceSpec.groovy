package finance.service

import finance.domain.Description
import finance.repositories.DescriptionRepository
import finance.services.DescriptionService
import spock.lang.Specification

class DescriptionServiceSpec extends Specification {

    DescriptionRepository descriptionRepository = Mock()
    DescriptionService service = new DescriptionService(descriptionRepository)

    def 'descriptions delegates to repository'() {
        given:
        Description d = new Description(descriptionName: 'walmart')
        descriptionRepository.descriptions() >> [d]

        when:
        List<Description> result = service.descriptions()

        then:
        result == [d]
    }

    def 'descriptionInsert sets timestamps and inserts new description'() {
        given:
        Description description = new Description(descriptionName: 'walmart', owner: 'henninb@gmail.com', activeStatus: true)
        descriptionRepository.description('walmart') >> null

        when:
        Description result = service.descriptionInsert(description)

        then:
        1 * descriptionRepository.descriptionInsert(description)
        result == description
        result.dateAdded != null
        result.dateUpdated != null
    }

    def 'descriptionInsert returns existing description without re-inserting'() {
        given:
        Description existing = new Description(descriptionName: 'walmart', descriptionId: 1L)
        descriptionRepository.description('walmart') >> existing

        when:
        Description result = service.descriptionInsert(new Description(descriptionName: 'walmart'))

        then:
        0 * descriptionRepository.descriptionInsert(_)
        result == existing
    }

    def 'descriptionUpdate throws when description not found'() {
        given:
        descriptionRepository.description('missing') >> null

        when:
        service.descriptionUpdate(new Description(descriptionName: 'missing'))

        then:
        thrown(RuntimeException)
    }

    def 'descriptionUpdate delegates and returns refreshed description'() {
        given:
        Description existing = new Description(descriptionName: 'walmart')
        Description updated = new Description(descriptionName: 'walmart', activeStatus: false)
        descriptionRepository.description('walmart') >>> [existing, updated]

        when:
        Description result = service.descriptionUpdate(new Description(descriptionName: 'walmart', activeStatus: false))

        then:
        1 * descriptionRepository.descriptionUpdate(_)
        result.activeStatus == false
    }

    def 'descriptionDelete returns false when description not found'() {
        given:
        descriptionRepository.description('missing') >> null

        expect:
        !service.descriptionDelete('missing')
    }

    def 'descriptionDelete returns true when description exists'() {
        given:
        descriptionRepository.description('walmart') >> new Description(descriptionName: 'walmart')
        descriptionRepository.descriptionDelete('walmart') >> true

        expect:
        service.descriptionDelete('walmart')
    }

    def 'descriptionsMerge delegates to repository'() {
        given:
        descriptionRepository.descriptionsMerge(['walmart', 'wal-mart'], 'walmart') >> true

        expect:
        service.descriptionsMerge(['walmart', 'wal-mart'], 'walmart')
    }
}
