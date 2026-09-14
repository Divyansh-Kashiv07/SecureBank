package com.securebank.service;

import com.securebank.core.Account;
import com.securebank.core.Beneficiary;
import com.securebank.core.CurrentAccount;
import com.securebank.core.Customer;
import com.securebank.core.SavingsAccount;
import com.securebank.exceptions.AccountInactiveException;
import com.securebank.exceptions.DailyLimitExceededException;
import com.securebank.exceptions.InsufficientBalanceException;
import com.securebank.loans.Loan;
import com.securebank.loans.LoanProcessor;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.BeneficiaryRepository;
import com.securebank.repository.CustomerRepository;
import com.securebank.transactions.Transaction;
import com.securebank.transactions.TransactionLogger;
import com.securebank.utils.FileIOHelper;
import com.securebank.utils.IDGenerator;
import com.securebank.utils.PasswordHasher;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BankService — the application/business layer.
 *
 * This class is the single home of the bank's use cases and of every
 * authorization rule. It knows nothing about sockets, threads or the GUI:
 * the transport layer ({@code ClientHandler}) hands it plain values and
 * writes back whatever string it returns, so the same service could be
 * driven by a REST controller, a CLI or a test without changes.
 *
 * RESPONSIBILITIES
 * - Authentication (PBKDF2 verification + transparent legacy-hash upgrade).
 * - Money movement: deposit, withdraw, transfer.
 * - Account, loan, statement and beneficiary queries.
 * - AUTHORIZATION: every operation is scoped to an authenticated customer id,
 *   and accounts/loans/beneficiaries are checked against that owner before
 *   anything is read or mutated (IDOR protection).
 * - Persistence orchestration: one atomic save path for the whole bank.
 *
 * RESPONSE CONTRACT (unchanged from the wire protocol):
 *   success → "OK|data..."      failure → "ERROR|human readable message"
 * Returning the response as a string keeps the protocol in transport and the
 * logic here; business failures are values, not exceptions.
 */
public class BankService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final LoanProcessor loanProcessor;
    private final BeneficiaryRepository beneficiaryRepository;
    private final TransactionLogger transactionLogger;

    /**
     * Creates the service over the bank's shared repositories.
     *
     * @param accountRepository     account storage (shared across connections)
     * @param customerRepository    customer storage
     * @param loanProcessor         loan engine and loan storage
     * @param beneficiaryRepository saved-payee storage
     * @param transactionLogger     asynchronous transaction log daemon
     */
    public BankService(AccountRepository accountRepository,
                       CustomerRepository customerRepository,
                       LoanProcessor loanProcessor,
                       BeneficiaryRepository beneficiaryRepository,
                       TransactionLogger transactionLogger) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.loanProcessor = loanProcessor;
        this.beneficiaryRepository = beneficiaryRepository;
        this.transactionLogger = transactionLogger;
    }

    // ==================== LIFECYCLE ====================

    /**
     * Loads all persisted state into memory.
     *
     * @throws IOException if the data directory cannot be created
     */
    public void loadAll() throws IOException {
        FileIOHelper.ensureDataDirectory();
        accountRepository.loadFromFile();
        customerRepository.loadFromFile();
        loanProcessor.loadFromFile();
        beneficiaryRepository.loadFromFile();
    }

    /**
     * Persists every repository atomically.
     *
     * @throws IOException if any write fails — the previous files stay intact
     */
    public void saveAll() throws IOException {
        accountRepository.saveToFile();
        customerRepository.saveToFile();
        loanProcessor.saveToFile();
        beneficiaryRepository.saveToFile();
    }

    /**
     * Resumes ID generation from the highest ids already on disk so a restart
     * can never reissue an existing id.
     */
    public void updateIdCounters() {
        int maxAcc = 1000, maxCust = 100, maxTxn = 0, maxLoan = 0, maxBene = 0;

        for (Account acc : accountRepository.getAllAccounts()) {
            maxAcc = Math.max(maxAcc, IDGenerator.extractNumber(acc.getAccountNumber()));
            for (Transaction txn : acc.getTransactionHistory()) {
                maxTxn = Math.max(maxTxn, IDGenerator.extractNumber(txn.getTransactionId()));
            }
        }
        for (Customer cust : customerRepository.getAllCustomers()) {
            maxCust = Math.max(maxCust, IDGenerator.extractNumber(cust.getCustomerId()));
        }
        for (Loan loan : loanProcessor.getAllLoans()) {
            maxLoan = Math.max(maxLoan, IDGenerator.extractNumber(loan.getLoanId()));
        }
        for (Beneficiary bene : beneficiaryRepository.getAll()) {
            maxBene = Math.max(maxBene, IDGenerator.extractNumber(bene.getBeneficiaryId()));
        }

        IDGenerator.initializeCounters(maxAcc, maxCust, maxTxn, maxLoan, maxBene);
    }

    /** @return true when no customer exists yet (first run) */
    public boolean isEmpty() {
        return customerRepository.size() == 0;
    }

    /**
     * Seeds the first-run demonstration data set and persists it.
     *
     * SECURITY: demo PINs are salted PBKDF2 hashes from the start, so no
     * plaintext PIN ever reaches the data files.
     */
    public void seedDemoData() {
        System.out.println("[Service] First run detected — seeding demo data...");

        Customer customer1 = register("CUSTOMER-1", "Divyansh Kashiv",
                "divyansh@securebank.com", "9876543210", "Greater Noida, UP", "1234");
        openDemoAccount(customer1, new SavingsAccount(
                "ACC-001001", customer1.getName(), customer1.getCustomerId(), 25000.0));
        openDemoAccount(customer1, new CurrentAccount(
                "ACC-001002", customer1.getName(), customer1.getCustomerId(), 50000.0));

        Customer customer2 = register("CUSTOMER-2", "Priya Sharma",
                "priya@securebank.com", "9876543211", "Noida, UP", "5678");
        openDemoAccount(customer2, new SavingsAccount(
                "ACC-001003", customer2.getName(), customer2.getCustomerId(), 15000.0));

        Customer customer3 = register("CUSTOMER-3", "Rahul Verma",
                "rahul@securebank.com", "9876543212", "Delhi, India", "9012");
        openDemoAccount(customer3, new SavingsAccount(
                "ACC-001004", customer3.getName(), customer3.getCustomerId(), 35000.0));

        beneficiaryRepository.addBeneficiary(new Beneficiary(IDGenerator.generateBeneficiaryId(),
                "CUSTOMER-1", "Priya Sharma", "ACC-001003", "SecureBank", "Priya"));
        beneficiaryRepository.addBeneficiary(new Beneficiary(IDGenerator.generateBeneficiaryId(),
                "CUSTOMER-1", "Rahul Verma", "ACC-001004", "SecureBank", "Rahul — Rent"));

        try {
            saveAll();
        } catch (IOException e) {
            System.err.println("[Service] CRITICAL: could not persist seed data: " + e.getMessage());
        }

        System.out.println("[Service] Demo logins: CUSTOMER-1/1234 · CUSTOMER-2/5678 · CUSTOMER-3/9012");
    }

    /** Creates a demo customer with a hashed PIN and stores it. */
    private Customer register(String customerId, String name, String email, String phone,
                              String address, String pin) {
        Customer customer = new Customer(customerId, name, email, phone, address,
                PasswordHasher.hash(pin));
        customerRepository.addCustomer(customer);
        return customer;
    }

    /** Stores an account and links it to its owning customer. */
    private void openDemoAccount(Customer customer, Account account) {
        accountRepository.addAccount(account);
        customer.addAccount(account.getAccountNumber());
    }

    /** @return the number of accounts held in memory */
    public int getAccountCount() {
        return accountRepository.size();
    }

    /** @return the number of customers held in memory */
    public int getCustomerCount() {
        return customerRepository.size();
    }

    /** Starts the asynchronous transaction log daemon. */
    public void startLogger() {
        transactionLogger.start();
    }

    /** Flushes and stops the transaction log daemon. */
    public void stopLogger() {
        transactionLogger.stop();
    }

    // ==================== AUTHENTICATION ====================

    /**
     * Verifies a customer's credentials.
     *
     * SECURITY:
     * - Returns null for an unknown customer, an inactive customer and a wrong
     *   PIN alike, so callers can answer with one generic message and an
     *   attacker cannot enumerate valid customer IDs.
     * - A successful login silently upgrades a legacy plaintext PIN record to a
     *   salted PBKDF2 hash.
     *
     * @return the authenticated customer, or null when the credentials are invalid
     */
    public Customer authenticate(String customerId, String pin) {
        Customer customer = customerRepository.getCustomer(customerId);
        if (customer == null || !customer.isActive()) {
            return null;
        }
        if (!PasswordHasher.verify(pin, customer.getPin())) {
            return null;
        }
        if (!PasswordHasher.isHashed(customer.getPin())) {
            customer.setPin(PasswordHasher.hash(pin));
            persistQuietly();
        }
        return customer;
    }

    // ==================== ACCOUNT OPERATIONS ====================

    /**
     * Handles BALANCE — the balance of one account owned by the caller.
     */
    public String getBalance(String customerId, String accountNumber) {
        String denied = requireOwnedAccount(customerId, accountNumber);
        if (denied != null) return denied;

        Account account = accountRepository.getAccount(accountNumber);
        return "OK|" + String.format("%.2f", account.getBalance());
    }

    /**
     * Handles DEPOSIT — credits an owned account and logs the movement.
     */
    public String deposit(String customerId, String accountNumber, String amountText, String remarks) {
        String denied = requireOwnedAccount(customerId, accountNumber);
        if (denied != null) return denied;

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            return "ERROR|Invalid amount format";
        }

        Account account = accountRepository.getAccount(accountNumber);
        double newBalance;

        try {
            // Account.deposit() is synchronized — concurrent deposits cannot lose an update
            newBalance = (remarks == null || remarks.isEmpty())
                    ? account.deposit(amount)
                    : account.deposit(amount, remarks);
        } catch (AccountInactiveException e) {
            return "ERROR|" + e.getMessage();
        }

        String txnId = logLatest(account);
        persistQuietly();
        return "OK|" + String.format("%.2f", newBalance) + "|" + txnId;
    }

    /**
     * Handles WITHDRAW — debits an owned account, enforcing the balance,
     * daily-limit and frozen-account rules.
     */
    public String withdraw(String customerId, String accountNumber, String amountText) {
        String denied = requireOwnedAccount(customerId, accountNumber);
        if (denied != null) return denied;

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            return "ERROR|Invalid amount format";
        }

        Account account = accountRepository.getAccount(accountNumber);

        try {
            double newBalance = account.withdraw(amount);
            String txnId = logLatest(account);
            persistQuietly();
            return "OK|" + String.format("%.2f", newBalance) + "|" + txnId;

        } catch (InsufficientBalanceException e) {
            return "ERROR|" + e.getMessage();
        } catch (DailyLimitExceededException e) {
            return "ERROR|" + e.getMessage();
        } catch (AccountInactiveException e) {
            return "ERROR|" + e.getMessage();
        }
    }

    /**
     * Handles TRANSFER — moves money from an owned account to any account.
     *
     * Only the SOURCE must belong to the caller: paying somebody else is the
     * whole point of a transfer.
     */
    public String transfer(String customerId, String fromAccount, String toAccount, String amountText) {
        String denied = requireOwnedAccount(customerId, fromAccount);
        if (denied != null) return denied;

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            return "ERROR|Invalid amount format";
        }

        Account source = accountRepository.getAccount(fromAccount);
        Account target = accountRepository.getAccount(toAccount);

        if (source == null) return "ERROR|Source account not found: " + fromAccount;
        if (target == null) return "ERROR|Target account not found: " + toAccount;

        try {
            // transferTo() orders its own locks so opposite-direction transfers cannot deadlock
            source.transferTo(target, amount);

            logLatest(source);
            logLatest(target);
            persistQuietly();

            return "OK|" + String.format("%.2f", source.getBalance()) + "|" + lastTxnId(source);

        } catch (InsufficientBalanceException e) {
            return "ERROR|" + e.getMessage();
        } catch (DailyLimitExceededException e) {
            return "ERROR|" + e.getMessage();
        } catch (AccountInactiveException e) {
            return "ERROR|" + e.getMessage();
        }
    }

    /**
     * Handles ACCOUNT_INFO — holder, type, balance and interest rate.
     */
    public String getAccountInfo(String customerId, String accountNumber) {
        String denied = requireOwnedAccount(customerId, accountNumber);
        if (denied != null) return denied;

        Account account = accountRepository.getAccount(accountNumber);

        String rate;
        if (account instanceof SavingsAccount savings) {
            rate = String.valueOf(savings.getInterestRate());
        } else if (account instanceof CurrentAccount current) {
            rate = String.valueOf(current.getInterestRate());
        } else {
            rate = "0.0";
        }

        return "OK|" + account.getHolderName()
                + "|" + account.getAccountType()
                + "|" + String.format("%.2f", account.getBalance())
                + "|" + rate;
    }

    /**
     * Handles INTEREST — the interest accrued on an owned account.
     */
    public String calculateInterest(String customerId, String accountNumber) {
        String denied = requireOwnedAccount(customerId, accountNumber);
        if (denied != null) return denied;

        Account account = accountRepository.getAccount(accountNumber);
        return "OK|" + String.format("%.2f", account.calculateInterest());
    }

    /**
     * Handles HISTORY — every transaction of an owned account, in order.
     */
    public String getHistory(String customerId, String accountNumber) {
        String denied = requireOwnedAccount(customerId, accountNumber);
        if (denied != null) return denied;

        List<Transaction> history = accountRepository.getAccount(accountNumber).getTransactionHistory();
        if (history.isEmpty()) {
            return "OK|EMPTY";
        }
        return "OK|" + history.stream()
                .map(Transaction::toFileString)
                .collect(Collectors.joining(";"));
    }

    /**
     * Handles ACCOUNTS — the caller's own accounts, as "number,type,balance" records.
     *
     * @param requestedCustomerId the id the client asked for; must be the caller
     */
    public String getAccounts(String customerId, String requestedCustomerId) {
        if (!customerId.equals(requestedCustomerId)) {
            return "ERROR|Not authorized to view another customer's accounts";
        }

        Customer customer = customerRepository.getCustomer(customerId);
        if (customer == null) {
            return "ERROR|Customer not found: " + customerId;
        }

        StringBuilder result = new StringBuilder();
        for (String accountNumber : customer.getAccountNumbers()) {
            Account acc = accountRepository.getAccount(accountNumber);
            if (acc != null) {
                if (result.length() > 0) result.append(";");
                result.append(acc.getAccountNumber()).append(",");
                result.append(acc.getAccountType()).append(",");
                result.append(String.format("%.2f", acc.getBalance()));
            }
        }
        return result.length() > 0 ? "OK|" + result : "OK|EMPTY";
    }

    /**
     * Handles CREATE_ACCOUNT — opens a new account for the caller.
     */
    public String createAccount(String customerId, String accountType, String initialBalanceText) {
        double initialBalance;
        try {
            initialBalance = Double.parseDouble(initialBalanceText);
        } catch (NumberFormatException e) {
            return "ERROR|Invalid initial balance";
        }

        Customer customer = customerRepository.getCustomer(customerId);
        if (customer == null) {
            return "ERROR|Customer not found: " + customerId;
        }

        Account account;
        if ("Savings".equalsIgnoreCase(accountType)) {
            account = new SavingsAccount(IDGenerator.generateAccountNumber(),
                    customer.getName(), customerId, initialBalance);
        } else if ("Current".equalsIgnoreCase(accountType)) {
            account = new CurrentAccount(IDGenerator.generateAccountNumber(),
                    customer.getName(), customerId, initialBalance);
        } else {
            return "ERROR|Invalid account type. Use 'Savings' or 'Current'.";
        }

        accountRepository.addAccount(account);
        customer.addAccount(account.getAccountNumber());
        persistQuietly();

        return "OK|" + account.getAccountNumber();
    }

    // ==================== LOANS ====================

    /**
     * Handles LOAN_APPLY — applies, auto-approves (demo policy) and disburses
     * a loan for the caller.
     *
     * @param requestedCustomerId the id the client asked for; must be the caller
     */
    public String applyForLoan(String customerId, String requestedCustomerId, String accountNumber,
                               String amountText, String tenureText, String purpose) {
        if (!customerId.equals(requestedCustomerId)) {
            return "ERROR|Not authorized to apply for a loan on behalf of another customer";
        }

        String denied = requireOwnedAccount(customerId, accountNumber);
        if (denied != null) return denied;

        double amount;
        int tenure;
        try {
            amount = Double.parseDouble(amountText);
            tenure = Integer.parseInt(tenureText);
        } catch (NumberFormatException e) {
            return "ERROR|Invalid amount or tenure format";
        }

        Account account = accountRepository.getAccount(accountNumber);
        Loan loan = loanProcessor.applyForLoan(customerId, accountNumber, amount,
                tenure, purpose, account);

        if (loan == null) {
            return "ERROR|Loan application rejected. Check eligibility requirements.";
        }

        // Auto-approve for the demo (a real deployment would queue a manual review)
        loanProcessor.approveLoan(loan.getLoanId());
        loanProcessor.disburseLoan(loan.getLoanId(), account);
        persistQuietly();

        return "OK|" + loan.getLoanId() + "|" + String.format("%.2f", loan.getEmi());
    }

    /**
     * Handles LOAN_STATUS — the caller's own loans.
     *
     * @param requestedCustomerId the id the client asked for; must be the caller
     */
    public String getLoans(String customerId, String requestedCustomerId) {
        if (!customerId.equals(requestedCustomerId)) {
            return "ERROR|Not authorized to view another customer's loans";
        }

        List<Loan> loans = loanProcessor.getLoansByCustomer(customerId);
        if (loans.isEmpty()) {
            return "OK|EMPTY";
        }
        return "OK|" + loans.stream()
                .map(Loan::toFileString)
                .collect(Collectors.joining(";"));
    }

    // ==================== BENEFICIARIES ====================

    /**
     * Handles BENEFICIARY_LIST — the caller's saved payees, one "id|name|
     * account|bank|nickname" record per payee, separated by ";".
     */
    public String getBeneficiaries(String customerId) {
        List<Beneficiary> beneficiaries = beneficiaryRepository.getByCustomer(customerId);
        if (beneficiaries.isEmpty()) {
            return "OK|EMPTY";
        }
        return "OK|" + beneficiaries.stream()
                .map(Beneficiary::toFileString)
                .collect(Collectors.joining(";"));
    }

    /**
     * Handles BENEFICIARY_ADD — saves a new payee for the caller.
     *
     * Validation happens here (never trust the client): the payee name and
     * account number are required, the account cannot be one of the caller's
     * own accounts, and a payee can only be saved once per account number.
     * Delimiter characters are stripped so a name can never corrupt the record
     * encoding.
     */
    public String addBeneficiary(String customerId, String name, String accountNumber,
                                 String bankName, String nickname) {
        String cleanName = sanitize(name);
        String cleanAccount = sanitize(accountNumber);
        String cleanBank = sanitize(bankName);
        String cleanNickname = sanitize(nickname);

        if (cleanName.isEmpty() || cleanAccount.isEmpty()) {
            return "ERROR|Payee name and account number are required";
        }

        Customer customer = customerRepository.getCustomer(customerId);
        if (customer != null && customer.getAccountNumbers().contains(cleanAccount)) {
            return "ERROR|That account belongs to you — no need to save it as a payee";
        }
        if (beneficiaryRepository.isDuplicate(customerId, cleanAccount)) {
            return "ERROR|That account is already saved as a payee";
        }

        Beneficiary beneficiary = new Beneficiary(
                IDGenerator.generateBeneficiaryId(), customerId,
                cleanName, cleanAccount,
                cleanBank.isEmpty() ? "SecureBank" : cleanBank,
                cleanNickname);

        beneficiaryRepository.addBeneficiary(beneficiary);
        persistQuietly();

        return "OK|" + beneficiary.getBeneficiaryId();
    }

    /**
     * Handles BENEFICIARY_REMOVE — deletes a payee the caller owns.
     */
    public String removeBeneficiary(String customerId, String beneficiaryId) {
        Beneficiary beneficiary = beneficiaryRepository.getBeneficiary(beneficiaryId);
        if (beneficiary == null) {
            return "ERROR|Beneficiary not found: " + beneficiaryId;
        }
        if (!beneficiary.getCustomerId().equals(customerId)) {
            System.out.println("[Service] AUTHORIZATION DENIED: customer " + customerId
                    + " tried to remove beneficiary " + beneficiaryId);
            return "ERROR|Not authorized to remove this beneficiary";
        }

        beneficiaryRepository.deleteBeneficiary(beneficiaryId);
        persistQuietly();
        return "OK|Beneficiary removed";
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * AUTHORIZATION: returns an error response unless the account exists and is
     * owned by the authenticated customer.
     *
     * @return null when access is allowed, otherwise the response to send back
     */
    private String requireOwnedAccount(String customerId, String accountNumber) {
        Account account = accountRepository.getAccount(accountNumber);
        if (account == null) {
            return "ERROR|Account not found: " + accountNumber;
        }
        if (!account.getCustomerId().equals(customerId)) {
            System.out.println("[Service] AUTHORIZATION DENIED: customer " + customerId
                    + " requested account " + accountNumber);
            return "ERROR|Not authorized to access this account";
        }
        return null;
    }

    /**
     * Queues a transaction from an account onto the async log daemon.
     *
     * @return the logged transaction id, or "N/A" when nothing was recorded
     */
    private String logLatest(Account account) {
        List<Transaction> history = account.getTransactionHistory();
        if (history.isEmpty()) return "N/A";
        Transaction last = history.get(history.size() - 1);
        transactionLogger.log(last);
        return last.getTransactionId();
    }

    /** @return the id of the most recent transaction on an account, or "N/A" */
    private String lastTxnId(Account account) {
        List<Transaction> history = account.getTransactionHistory();
        return history.isEmpty() ? "N/A" : history.get(history.size() - 1).getTransactionId();
    }

    /**
     * RELIABILITY: persists the bank, reporting failures loudly.
     *
     * The in-memory operation has ALREADY succeeded and stays authoritative:
     * returning an error would make the client retry an operation that did
     * happen (double-crediting money). Atomic saves guarantee a failed write
     * leaves the previous files intact.
     */
    private void persistQuietly() {
        try {
            saveAll();
        } catch (IOException e) {
            System.err.println("[Service] CRITICAL: data persistence failed: " + e.getMessage());
        }
    }

    /**
     * Strips the protocol's field and record delimiters (plus newlines) from
     * user-supplied text so a value can never break the record encoding.
     */
    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replace("|", " ").replace(";", " ")
                .replace("\r", " ").replace("\n", " ").trim();
    }
}
