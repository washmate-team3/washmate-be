package swp391.carwash.enums;

public enum InsightSeverity {
    CRITICAL(0),
    WARNING(1),
    OPPORTUNITY(2),
    POSITIVE(3);

    private final int priority;

    InsightSeverity(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
