package com.securebank.core;

/**
 * Beneficiary — a saved payee belonging to one customer.
 *
 * A beneficiary lets a customer transfer money to an account they use often
 * without re-typing the details every time. Beneficiaries are owned by exactly
 * one customer, so the service layer can authorize access with a simple
 * ownership check (the same rule used for accounts and loans).
 *
 * PERSISTENCE FORMAT (one record per line, pipe-delimited):
 *   beneficiaryId|customerId|name|accountNumber|bankName|nickname
 */
public class Beneficiary {

    private final String beneficiaryId;
    private final String customerId;
    private final String name;
    private final String accountNumber;
    private final String bankName;
    private final String nickname;

    /**
     * Creates a beneficiary.
     *
     * @param beneficiaryId unique id (e.g., "BENE-000001")
     * @param customerId    the owning customer
     * @param name          the payee's account holder name
     * @param accountNumber the payee's account number
     * @param bankName      the payee's bank
     * @param nickname      optional short label shown in lists
     */
    public Beneficiary(String beneficiaryId, String customerId, String name,
                       String accountNumber, String bankName, String nickname) {
        this.beneficiaryId = beneficiaryId;
        this.customerId = customerId;
        this.name = name;
        this.accountNumber = accountNumber;
        this.bankName = bankName;
        this.nickname = nickname;
    }

    public String getBeneficiaryId() { return beneficiaryId; }

    public String getCustomerId() { return customerId; }

    public String getName() { return name; }

    public String getAccountNumber() { return accountNumber; }

    public String getBankName() { return bankName; }

    public String getNickname() { return nickname; }

    /**
     * Returns the label shown in lists — the nickname when the customer set
     * one, otherwise the payee's name.
     */
    public String getDisplayName() {
        return (nickname == null || nickname.isBlank()) ? name : nickname;
    }

    /**
     * Serializes this beneficiary to a single pipe-delimited line.
     */
    public String toFileString() {
        return String.join("|", beneficiaryId, customerId, name, accountNumber, bankName, nickname);
    }

    /**
     * Parses a pipe-delimited line back into a Beneficiary.
     *
     * @return the parsed beneficiary, or null if the line is malformed
     */
    public static Beneficiary fromFileString(String line) {
        if (line == null) return null;
        String[] parts = line.split("\\|", -1);
        if (parts.length < 6) return null;
        return new Beneficiary(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
    }

    @Override
    public String toString() {
        return getDisplayName() + " (" + accountNumber + " · " + bankName + ")";
    }
}
