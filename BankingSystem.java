import java.util.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class BankAccount {
    private String accountId;
    private String holderName;
    private double balance;

    public BankAccount(String accountId, String holderName, double initialBalance) {
        this.accountId = accountId;
        this.holderName = holderName;
        this.balance = initialBalance;
        logAccountCreation();
    }

    public synchronized void deposit(double amount) {
        System.out.println(Thread.currentThread().getName() + ": Depositing " + amount);
        balance += amount;
        System.out.println(Thread.currentThread().getName() + ": Deposit complete. New balance = " + balance);
    }

    public synchronized void withdraw(double amount) {
        System.out.println(Thread.currentThread().getName() + ": Withdrawing " + amount);
        if (amount <= balance) {
            balance -= amount;
            System.out.println(Thread.currentThread().getName() + ": Withdrawal complete. New balance = " + balance);
        } else {
            System.out.println(Thread.currentThread().getName() + ": Withdrawal failed. Insufficient funds.");
        }
    }

    public synchronized double getBalance() {
        return balance;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getHolderName() {
        return holderName;
    }

    private void logAccountCreation() {
        String fileName = "account_" + accountId + "_info.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("Account ID: " + accountId + "\n");
            writer.write("Holder Name: " + holderName + "\n");
            writer.write("Initial Balance: " + balance + "\n");
            writer.write("Account Created: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } catch (IOException e) {
            System.out.println("Error writing account info: " + e.getMessage());
        }
    }
}

class TransactionTask implements Runnable {
    private BankAccount account;
    private double amount;
    private boolean isDeposit;

    public TransactionTask(BankAccount account, double amount, boolean isDeposit) {
        this.account = account;
        this.amount = amount;
        this.isDeposit = isDeposit;
    }

    private void logTransaction(String message) {
        String fileName = "transactions_" + account.getAccountId() + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String dateTime = LocalDateTime.now().format(formatter);
            writer.write(dateTime + " - " + message);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Error logging transaction: " + e.getMessage());
        }
    }

    public void run() {
        if (isDeposit) {
            account.deposit(amount);
            logTransaction(Thread.currentThread().getName() + ": Deposited " + amount + ". New balance: " + account.getBalance());
        } else {
            double beforeBalance = account.getBalance();
            account.withdraw(amount);
            if (beforeBalance >= amount) {
                logTransaction(Thread.currentThread().getName() + ": Withdrew " + amount + ". New balance: " + account.getBalance());
            } else {
                logTransaction(Thread.currentThread().getName() + ": Withdrawal of " + amount + " failed due to insufficient funds.");
            }
        }
    }
}

public class BankingSystem {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        HashMap<String, BankAccount> accounts = new HashMap<>();

        System.out.print("Enter number of users: ");
        int n = scanner.nextInt();
        scanner.nextLine();

        for (int i = 0; i < n; i++) {
            System.out.print("\nEnter Account ID: ");
            String id = scanner.nextLine();
            System.out.print("Enter Holder Name: ");
            String name = scanner.nextLine();
            System.out.print("Enter Initial Balance: ");
            double balance = scanner.nextDouble();
            scanner.nextLine();

            accounts.put(id, new BankAccount(id, name, balance));
        }

        boolean exit = false;
        while (!exit) {
            System.out.println("\n--- Banking Menu ---");
            System.out.println("1. Deposit");
            System.out.println("2. Withdraw");
            System.out.println("3. Check Balance");
            System.out.println("4. View Transaction History");
            System.out.println("5. Save Transaction History to File");
            System.out.println("6. Exit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 6) {
                exit = true;
                System.out.println("Saving account summaries...");
                saveSummary(accounts);
                System.out.println("Exiting program.");
                break;
            }

            System.out.print("Enter Account ID: ");
            String accId = scanner.nextLine();
            BankAccount selected = accounts.get(accId);

            if (selected == null) {
                System.out.println("Account not found.");
                continue;
            }

            switch (choice) {
                case 1:
                    System.out.print("Enter deposit amount: ");
                    double depAmt = scanner.nextDouble();
                    scanner.nextLine();
                    Thread depositThread = new Thread(new TransactionTask(selected, depAmt, true), "DepositThread-" + accId);
                    depositThread.start();
                    try {
                        depositThread.join();
                    } catch (InterruptedException e) {
                    }
                    break;

                case 2:
                    System.out.print("Enter withdrawal amount: ");
                    double witAmt = scanner.nextDouble();
                    scanner.nextLine();
                    Thread withdrawThread = new Thread(new TransactionTask(selected, witAmt, false), "WithdrawThread-" + accId);
                    withdrawThread.start();
                    try {
                        withdrawThread.join();
                    } catch (InterruptedException e) {
                    }
                    break;

                case 3:
                    System.out.println("Account Holder: " + selected.getHolderName());
                    System.out.println("Current Balance: " + selected.getBalance());
                    break;

                case 4:
                    viewTransactionHistory(accId);
                    break;

                case 5:
                    saveTransactionHistoryToFile(accId);
                    break;

                default:
                    System.out.println("Invalid option.");
            }
        }
        scanner.close();
    }

    public static void viewTransactionHistory(String accountId) {
        String fileName = "transactions_" + accountId + ".txt";
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("No transactions found for this account.");
            return;
        }

        System.out.println("\n--- Transaction History for Account ID: " + accountId + " ---");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean hasData = false;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                hasData = true;
            }
            if (!hasData) {
                System.out.println("No transactions yet.");
            }
        } catch (IOException e) {
            System.out.println("Error reading transaction history: " + e.getMessage());
        }
    }

    public static void saveTransactionHistoryToFile(String accountId) {
        String sourceFileName = "transactions_" + accountId + ".txt";
        String destinationFileName = "transaction_history_" + accountId + ".txt";
        File sourceFile = new File(sourceFileName);
        if (!sourceFile.exists()) {
            System.out.println("No transaction history found for account: " + accountId);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("Transaction history for account " + accountId + " saved to " + destinationFileName);
        } catch (IOException e) {
            System.out.println("Error saving transaction history: " + e.getMessage());
        }
    }

    public static void saveSummary(HashMap<String, BankAccount> accounts) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("account_summary.txt"))) {
            for (BankAccount acc : accounts.values()) {
                writer.write("Account ID: " + acc.getAccountId());
                writer.newLine();
                writer.write("Holder Name: " + acc.getHolderName());
                writer.newLine();
                writer.write("Final Balance: " + acc.getBalance());
                writer.newLine();
                writer.write("----------------------------");
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error writing account summary: " + e.getMessage());
        }
    }
}