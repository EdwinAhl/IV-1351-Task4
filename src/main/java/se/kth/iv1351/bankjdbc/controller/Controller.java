/*
 * The MIT License (MIT)
 * Copyright (c) 2020 Leif Lindbäck
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction,including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so,subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package se.kth.iv1351.bankjdbc.controller;

import java.util.List;

import se.kth.iv1351.bankjdbc.integration.SchoolDAO;
import se.kth.iv1351.bankjdbc.integration.SchoolDBException;
import se.kth.iv1351.bankjdbc.model.PersonDTO;
import se.kth.iv1351.bankjdbc.model.AccountException;

/**
 * This is the application's only controller, all calls to the model pass here.
 * The controller is also responsible for calling the DAO. Typically, the
 * controller first calls the DAO to retrieve data (if needed), then operates on
 * the data, and finally tells the DAO to store the updated data (if any).
 */
public class Controller {
    private final SchoolDAO bankDb;

    /**
     * Creates a new instance, and retrieves a connection to the database.
     * 
     * @throws SchoolDBException If unable to connect to the database.
     */
    public Controller() throws SchoolDBException {
        bankDb = new SchoolDAO();
    }

    /**
     * Lists all accounts in the whole bank.
     * 
     * @return A list containing all accounts. The list is empty if there are no
     *         accounts.
     * @throws AccountException If unable to retrieve accounts.
     */
    public List<? extends PersonDTO> getAllAccounts() throws AccountException {
        try {
            return bankDb.getAllPeople();
        } catch (Exception e) {
            throw new AccountException("Unable to list accounts.", e);
        }
    }


//    /**
//     * Withdraws the specified amount from the account with the specified account
//     * number.
//     *
//     * @param acctNo The number of the account from which to withdraw.
//     * @param amt    The amount to withdraw.
//     * @throws RejectedException If not allowed to withdraw the specified amount.
//     * @throws AccountException  If failed to withdraw.
//     */
//    public void withdraw(String acctNo, int amt) throws RejectedException, AccountException {
//        String failureMsg = "Could not withdraw from account: " + acctNo;
//
//        if (acctNo == null) {
//            throw new AccountException(failureMsg);
//        }
//
//        try {
//            Account acct = bankDb.findAccountByAcctNo(acctNo, true);
//            acct.withdraw(amt);
//            bankDb.updateAccount(acct);
//        } catch (BankDBException bdbe) {
//            throw new AccountException(failureMsg, bdbe);
//        } catch (Exception e) {
//            commitOngoingTransaction(failureMsg);
//            throw e;
//        }
//    }


    private void commitOngoingTransaction(String failureMsg) throws AccountException {
        try {
            bankDb.commit();
        } catch (SchoolDBException bdbe) {
            throw new AccountException(failureMsg, bdbe);
        }
    }
}
