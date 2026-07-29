package com.securebank.core;

import com.securebank.exceptions.InsufficientBalanceException;
import com.securebank.exceptions.DailyLimitExceededException;

/**
 * Transferable Interface — defines the contract for any account that supports fund transfers.
 *
 * RUBRIC: Unit 2 — Interface requirement.
 * Any class implementing this interface guarantees it can transfer funds to another Account.
 * Both SavingsAccount and CurrentAccount implement this interface.
 *
 * VIVA NOTE: Interfaces define "what" a class can do (capability), while abstract classes
 * define "what" a class IS (identity). Transferable is a capability — an Account CAN transfer.
 */
public interface Transferable {

    /**
     * Transfers the specified amount from this account to the target account.
     *
     * @param target the destination Account to receive the funds
     * @param amount the amount to transfer (must be positive)
     * @throws InsufficientBalanceException if this account doesn't have enough balance
     * @throws DailyLimitExceededException  if the daily transfer limit has been exceeded
     */
    void transferTo(Account target, double amount)
            throws InsufficientBalanceException, DailyLimitExceededException;
}
