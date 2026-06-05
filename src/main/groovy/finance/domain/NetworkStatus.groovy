package finance.domain

enum NetworkStatus {
    InNetwork("in_network"),
    OutOfNetwork("out_of_network"),
    Unknown("unknown")

    final String label

    NetworkStatus(String label) {
        this.label = label
    }

    @Override
    String toString() { label }

    static NetworkStatus fromString(String value) {
        values().find { it.label == value?.toLowerCase() }
            ?: { throw new IllegalArgumentException("Unknown NetworkStatus: $value") }()
    }
}
