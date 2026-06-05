package finance.repositories

import com.google.inject.Inject
import finance.domain.MedicalProvider
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import static org.jooq.generated.Tables.T_MEDICAL_PROVIDER

@Log
class MedicalProviderRepository {
    private final DSLContext dslContext

    @Inject
    MedicalProviderRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    List<MedicalProvider> medicalProviders() {
        return dslContext.selectFrom(T_MEDICAL_PROVIDER)
                .where(T_MEDICAL_PROVIDER.ACTIVE_STATUS.eq(true))
                .fetchInto(MedicalProvider)
    }

    MedicalProvider medicalProvider(Long providerId) {
        return dslContext.selectFrom(T_MEDICAL_PROVIDER)
                .where(T_MEDICAL_PROVIDER.PROVIDER_ID.equal(providerId))
                .fetchOneInto(MedicalProvider)
    }

    boolean medicalProviderInsert(MedicalProvider medicalProvider) {
        dslContext.newRecord(T_MEDICAL_PROVIDER, medicalProvider).store()
        return true
    }

    boolean medicalProviderUpdate(MedicalProvider medicalProvider) {
        dslContext.update(T_MEDICAL_PROVIDER)
                .set(T_MEDICAL_PROVIDER.PROVIDER_NAME, medicalProvider.providerName)
                .set(T_MEDICAL_PROVIDER.PROVIDER_TYPE, medicalProvider.providerType)
                .set(T_MEDICAL_PROVIDER.SPECIALTY, medicalProvider.specialty)
                .set(T_MEDICAL_PROVIDER.NPI, medicalProvider.npi)
                .set(T_MEDICAL_PROVIDER.TAX_ID, medicalProvider.taxId)
                .set(T_MEDICAL_PROVIDER.ADDRESS_LINE1, medicalProvider.addressLine1)
                .set(T_MEDICAL_PROVIDER.ADDRESS_LINE2, medicalProvider.addressLine2)
                .set(T_MEDICAL_PROVIDER.CITY, medicalProvider.city)
                .set(T_MEDICAL_PROVIDER.STATE, medicalProvider.state)
                .set(T_MEDICAL_PROVIDER.ZIP_CODE, medicalProvider.zipCode)
                .set(T_MEDICAL_PROVIDER.COUNTRY, medicalProvider.country)
                .set(T_MEDICAL_PROVIDER.PHONE, medicalProvider.phone)
                .set(T_MEDICAL_PROVIDER.FAX, medicalProvider.fax)
                .set(T_MEDICAL_PROVIDER.EMAIL, medicalProvider.email)
                .set(T_MEDICAL_PROVIDER.WEBSITE, medicalProvider.website)
                .set(T_MEDICAL_PROVIDER.NETWORK_STATUS, medicalProvider.networkStatus)
                .set(T_MEDICAL_PROVIDER.BILLING_NAME, medicalProvider.billingName)
                .set(T_MEDICAL_PROVIDER.NOTES, medicalProvider.notes)
                .set(T_MEDICAL_PROVIDER.ACTIVE_STATUS, medicalProvider.activeStatus)
                .where(T_MEDICAL_PROVIDER.PROVIDER_ID.eq(medicalProvider.providerId))
                .execute()
        return true
    }

    boolean medicalProviderDelete(Long providerId) {
        dslContext.delete(T_MEDICAL_PROVIDER)
                .where(T_MEDICAL_PROVIDER.PROVIDER_ID.equal(providerId))
                .execute()
        return true
    }
}
