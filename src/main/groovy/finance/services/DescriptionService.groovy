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

    Description description(String descriptionName) {
        return descriptionRepository.description(descriptionName)
    }

    Description descriptionInsert(Description description) {
        description.dateUpdated = new Timestamp(System.currentTimeMillis())
        description.dateAdded = new Timestamp(System.currentTimeMillis())
        if (descriptionRepository.description(description.descriptionName)) {
            return descriptionRepository.description(description.descriptionName)
        }
        descriptionRepository.descriptionInsert(description)
        return description
    }

    Description descriptionUpdate(Description description) {
        Description existing = descriptionRepository.description(description.descriptionName)
        if (!existing) {
            throw new RuntimeException("description not found: ${description.descriptionName}")
        }
        descriptionRepository.descriptionUpdate(description)
        return descriptionRepository.description(description.descriptionName)
    }

    boolean descriptionDelete(String descriptionName) {
        Description existing = descriptionRepository.description(descriptionName)
        if (!existing) {
            return false
        }
        return descriptionRepository.descriptionDelete(descriptionName)
    }

    boolean descriptionsMerge(List<String> sourceNames, String targetName) {
        return descriptionRepository.descriptionsMerge(sourceNames, targetName)
    }
}
