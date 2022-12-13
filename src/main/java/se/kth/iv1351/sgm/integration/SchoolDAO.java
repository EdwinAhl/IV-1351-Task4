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

package se.kth.iv1351.sgm.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import se.kth.iv1351.sgm.model.Instrument;

/**
 * This data access object (DAO) encapsulates all database calls in the bank
 * application. No code outside this class shall have any knowledge about the
 * database.
 */
public class SchoolDAO {
//    private static final String HOLDER_TABLE_NAME = "holder";
//    private static final String HOLDER_PK_COLUMN_NAME = "holder_id";
//    private static final String HOLDER_COLUMN_NAME = "name";
//    private static final String ACCT_TABLE_NAME = "account";
//    private static final String ACCT_NO_COLUMN_NAME = "account_no";
//    private static final String BALANCE_COLUMN_NAME = "balance";
//    private static final String HOLDER_FK_COLUMN_NAME = HOLDER_PK_COLUMN_NAME;

    private Connection connection;
    //private PreparedStatement findAllPeopleStatement;

    /**
     * Constructs a new DAO object connected to the bank database.
     */
    public SchoolDAO() throws SchoolDBException {
        try {
            connectToBankDB();
            prepareStatements();
        } catch (ClassNotFoundException | SQLException exception) {
            throw new SchoolDBException("Could not connect to datasource.", exception);
        }
    }

    // Returns list of all rentable_instruments not in lease
    public List<Instrument> getInstruments(String type) throws SchoolDBException {
        String failureMsg = "Could not list instruments.";
        List<Instrument> instruments = new ArrayList<>();
        try (ResultSet result = findAllInstrument(type).executeQuery()) {
            while (result.next()) {
                instruments.add(new Instrument(
                        result.getInt("id"),
                        result.getInt("price"),
                        type,
                        result.getString("brand"),
                        result.getString("quality")));
            }
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        }
        return instruments;
    }

    // Adds lease
    public void rent(int student_id, int instrument_id, String end_day) throws SchoolDBException {
        String failureMsg = "Could not add lease.";
        try{
            ResultSet countResult = countRentals(student_id).executeQuery();
            countResult.next();
            if(countResult.getInt("count") < 2){  // Student has <2 leases
                ResultSet leaseResult =  insertLease(student_id, end_day).executeQuery();
                leaseResult.next();
                int lease_id = leaseResult.getInt("id");  // Create lease and return generated id
                updateInstrumentToLease(instrument_id, lease_id);  // Add lease_id to instrument
                connection.commit();
            }
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        }
    }

    // Terminates lease
    public void terminate(int lease_id) throws SchoolDBException {
        String failureMsg = "Could not terminate rental.";
        try{
            updateLease(lease_id);
            updateInstrumentFromLease(lease_id);
            connection.commit();
        } catch (SQLException sqle) {
            handleException(failureMsg, sqle);
        }
    }

    /**
     * Commits the current transaction.
     * 
     * @throws SchoolDBException If unable to commit the current transaction.
     * Will roll back transactions if errored
     */
    public void commit() throws SchoolDBException {
        try {
            connection.commit();
        } catch (SQLException e) {
            handleException("Failed to commit", e);
        }
    }

    private void connectToBankDB() throws ClassNotFoundException, SQLException {
        connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/sgm",
                                                 "postgres", "post");
        connection.setAutoCommit(false);
    }

    private void prepareStatements() throws SQLException {
        //findAllPeopleStatement = connection.prepareStatement("SELECT * FROM PERSON");
    }

    // Selects all instruments of given type
    private PreparedStatement findAllInstrument(String type) throws SQLException {
        return connection.prepareStatement(
                "SELECT t1.id, price, brand, quality " +
                    "FROM rentable_instrument as t1 " +
                    "LEFT JOIN lease AS t2 ON t2.id = t1.lease_id " +
                    "WHERE t2.id IS NULL AND type ='" + type + "'");
    }

    // Select counts all rentals from a given student
    private PreparedStatement countRentals(int student_id) throws SQLException {
        return connection.prepareStatement(
                    "SELECT COUNT(*) " +
                        "FROM student as t1 JOIN lease as t2 " +
                        "ON t1.id = t2.student_id " +
                        "WHERE t1.id = " + student_id);
    }

    // Inserts a lease
    private PreparedStatement insertLease(int student_id, String end_day) throws SQLException {
        return connection.prepareStatement(
                    "INSERT INTO lease(student_id, start_day, end_day) " +
                        "VALUES " +
                        "(" + student_id + ", " + "CURRENT_DATE" + ", '" + end_day + "')" +
                        "RETURNING id");
    }

    // Adds lease to instrument
    private PreparedStatement updateInstrumentToLease(int instrument_id, int lease_id) throws SQLException {
        System.out.println(
                "UPDATE rentable_instrument " +
                        "SET lease_id = " + lease_id + " " +
                        "WHERE id = " + instrument_id);
        return connection.prepareStatement(
                    "UPDATE rentable_instrument " +
                        "SET lease_id = " + lease_id + " " +
                        "WHERE id = " + instrument_id);
    }

    // Updates lease to set end_day as current day, meaning terminated
    private PreparedStatement updateLease(int lease_id) throws SQLException {
        return connection.prepareStatement(
              "UPDATE lease " +
                  "SET end_day = CURRENT_DAY " +
                  "WHERE id = " + lease_id);
    }

    // Removes lease from instrument so it is rentable again
    private PreparedStatement updateInstrumentFromLease(int lease_id) throws SQLException {
        return connection.prepareStatement(
                    "UPDATE rentable_instrument " +
                        "SET lease_id = null " +
                        "WHERE lease_id = " + lease_id);
    }

    private void handleException(String failureMsg, Exception cause) throws SchoolDBException {
        String completeFailureMsg = failureMsg;
        try {
            connection.rollback();
        } catch (SQLException rollbackExc) {
            completeFailureMsg = completeFailureMsg + 
            ". Also failed to rollback transaction because of: " + rollbackExc.getMessage();
        }

        if (cause != null) {
            throw new SchoolDBException(failureMsg, cause);
        } else {
            throw new SchoolDBException(failureMsg);
        }
    }

    private void closeResultSet(String failureMsg, ResultSet result) throws SchoolDBException {
        try {
            result.close();
        } catch (Exception e) {
            throw new SchoolDBException(failureMsg + " Could not close result set.", e);
        }
    }
}
