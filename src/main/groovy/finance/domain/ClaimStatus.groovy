package finance.domain

enum ClaimStatus {
    Submitted("submitted"),
    Processing("processing"),
    Approved("approved"),
    Denied("denied"),
    Paid("paid"),
    Closed("closed")

    final String label

    ClaimStatus(String label) {
        this.label = label
    }

    @Override
    String toString() { label }

    static ClaimStatus fromString(String value) {
        values().find { it.label == value?.toLowerCase() }
            ?: { throw new IllegalArgumentException("Unknown ClaimStatus: $value") }()
    }
}
