package finance.services


import finance.domain.Description
import finance.repositories.DescriptionRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Timestamp

@Log
@CompileStatic
class DescriptionService implements Service {

    private DescriptionRepository descriptionRepository

    @Inject
    DescriptionService(DescriptionRepository descriptionRepository) {
        this.descriptionRepository = descriptionRepository
    }

    List<Description> descriptions() {
        return descriptionRepository.descriptions()
    }

    Description descriptionInsert(Description description) {
        description.dateUpdated = new Timestamp(System.currentTimeMillis())
        description.dateAdded = new Timestamp(System.currentTimeMillis())
        if(descriptionRepository.description(description.descriptionName)) {
            return descriptionRepository.description(description.descriptionName)
        }
        descriptionRepository.descriptionInsert(description)
        return description
    }
}
