package finance.services

import finance.domain.MedicalProvider
import finance.repositories.MedicalProviderRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Timestamp

@Log
@CompileStatic
class MedicalProviderService implements Service {

    private MedicalProviderRepository medicalProviderRepository

    @Inject
    MedicalProviderService(MedicalProviderRepository medicalProviderRepository) {
        this.medicalProviderRepository = medicalProviderRepository
    }

    List<MedicalProvider> medicalProviders() {
        return medicalProviderRepository.medicalProviders()
    }

    MedicalProvider medicalProvider(Long providerId) {
        return medicalProviderRepository.medicalProvider(providerId)
    }

    MedicalProvider medicalProviderInsert(MedicalProvider medicalProvider) {
        medicalProvider.dateUpdated = new Timestamp(System.currentTimeMillis())
        medicalProvider.dateAdded = new Timestamp(System.currentTimeMillis())
        medicalProviderRepository.medicalProviderInsert(medicalProvider)
        log.info("inserted medical provider ${medicalProvider.providerName}")
        return medicalProvider
    }

    MedicalProvider medicalProviderUpdate(MedicalProvider medicalProvider) {
        MedicalProvider existing = medicalProviderRepository.medicalProvider(medicalProvider.providerId)
        if (!existing) {
            throw new RuntimeException("medical provider not found: ${medicalProvider.providerId}")
        }
        medicalProvider.dateUpdated = new Timestamp(System.currentTimeMillis())
        medicalProviderRepository.medicalProviderUpdate(medicalProvider)
        return medicalProviderRepository.medicalProvider(medicalProvider.providerId)
    }

    boolean medicalProviderDelete(Long providerId) {
        MedicalProvider existing = medicalProviderRepository.medicalProvider(providerId)
        if (!existing) {
            return false
        }
        return medicalProviderRepository.medicalProviderDelete(providerId)
    }
}
