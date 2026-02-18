package compliance.domain;

public class TransactionCategory {
    public String transactionId;
    public String category;        // e.g., "HOUSING", "FOOD_BEVERAGE", "INTERNATIONAL_TRANSFER", "INVESTMENT"
    public String subCategory;     // e.g., "Rent Payment", "Coffee Purchase"
    public String regulatoryFlags; // e.g., "BSA_CTR", "OFAC", "NONE"
    public String reasoning;

    public TransactionCategory() {}

    public TransactionCategory(String transactionId, String category, String subCategory,
                               String regulatoryFlags, String reasoning) {
        this.transactionId = transactionId;
        this.category = category;
        this.subCategory = subCategory;
        this.regulatoryFlags = regulatoryFlags;
        this.reasoning = reasoning;
    }

    public String getTransactionId() { return transactionId; }
    public String getCategory() { return category; }
    public String getSubCategory() { return subCategory; }
    public String getRegulatoryFlags() { return regulatoryFlags; }
    public String getReasoning() { return reasoning; }
}
