package finance.domain

enum FamilyRelationship {
    Self("self"),
    Spouse("spouse"),
    Child("child"),
    Dependent("dependent"),
    Other("other")

    final String label

    FamilyRelationship(String label) {
        this.label = label
    }

    @Override
    String toString() { label }

    static FamilyRelationship fromString(String value) {
        values().find { it.label == value?.toLowerCase() }
            ?: { throw new IllegalArgumentException("Unknown FamilyRelationship: $value") }()
    }
}
