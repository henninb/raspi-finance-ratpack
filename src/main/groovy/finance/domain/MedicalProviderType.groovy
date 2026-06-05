package finance.domain

enum MedicalProviderType {
    General("general"),
    Specialist("specialist"),
    Hospital("hospital"),
    Pharmacy("pharmacy"),
    Laboratory("laboratory"),
    Imaging("imaging"),
    UrgentCare("urgent_care"),
    Emergency("emergency"),
    MentalHealth("mental_health"),
    Dental("dental"),
    Vision("vision"),
    PhysicalTherapy("physical_therapy"),
    Other("other")

    final String label

    MedicalProviderType(String label) {
        this.label = label
    }

    @Override
    String toString() { label }

    static MedicalProviderType fromString(String value) {
        values().find { it.label == value?.toLowerCase() }
            ?: { throw new IllegalArgumentException("Unknown MedicalProviderType: $value") }()
    }
}
