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
 * This data access object (DAO) encapsulates all database calls in the school
 * application. No code outside this class shall have any knowledge about the
 * database.
 */
public class SchoolDAO {
    private static final String INSTRUMENT_COLUMN_ID = "id";
    private static final String INSTRUMENT_COLUMN_PRICE = "price";
    private static final String INSTRUMENT_COLUMN_BRAND = "brand";
    private static final String INSTRUMENT_COLUMN_QUALITY = "quality";
    private static final String COLUMN_COUNT = "count";
    private static final String LEASE_COLUMN_ID = "id";

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

    /**
     * @return A list of all rentable_instruments (not currently leased)
     */
    public List<Instrument> getInstruments(String type) throws SchoolDBException {
        String failureMsg = "Could not list instruments.";
        List<Instrument> instruments = new ArrayList<>();

        try (ResultSet result = getFindAllInstrumentsQuery(type).executeQuery()) {
            while (result.next()) {
                instruments.add(new Instrument(
                        result.getInt(INSTRUMENT_COLUMN_ID),
                        result.getInt(INSTRUMENT_COLUMN_PRICE),
                        type,
                        result.getString(INSTRUMENT_COLUMN_BRAND),
                        result.getString(INSTRUMENT_COLUMN_QUALITY)));
            }
            connection.commit();
        } catch (SQLException sqlException) {
            handleException(failureMsg, sqlException);
        }
        return instruments;
    }

    public int getStudentRentalCount(int studentId) throws SQLException, SchoolDBException {
        String failureMsg = "Could not get student rental count.";

        ResultSet countResult = getStudentRentalsCountQuery(studentId).executeQuery();
        countResult.next();
        int count = countResult.getInt(COLUMN_COUNT);
        closeResultSet(failureMsg, countResult);

        return count;
    }


    // Adds lease
    public void createRental(int studentId, int instrumentId, String endDay) throws SchoolDBException {
        String failureMsg = "Could not add lease.";
        try {
            ResultSet leaseResult = getLeaseCreatorQuery(studentId, endDay).executeQuery();
            leaseResult.next();
            int leaseId = leaseResult.getInt(LEASE_COLUMN_ID);  // Create lease and return generated id
            closeResultSet(failureMsg, leaseResult);

            getInstrumentLeaseUpdateQuery(instrumentId, leaseId).executeUpdate();  // Add leaseId to instrument
            connection.commit();
        } catch (SQLException sqlException) {
            handleException(failureMsg, sqlException);
        }
    }

    /**
     * Terminates the lease by setting the end date to today and removing the rental from the instrument
     **/
    public void terminateRental(int leaseId) throws SchoolDBException {
        // TODO this does not save which instrument was rented!
        String failureMsg = "Could not terminate rental.";
        try {
            getLeaseTerminationQuery(leaseId).executeUpdate();
            getInstrumentLeaseUpdateQuery(null, leaseId).executeUpdate();
            connection.commit();
        } catch (SQLException sqlException) {
            handleException(failureMsg, sqlException);
        }
    }

    /**
     * Commits the current transaction.
     *
     * @throws SchoolDBException If unable to commit the current transaction.
     *                           Will roll back transactions if errored
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

    /**
     * Selects all instruments of given type
     */
    private PreparedStatement getFindAllInstrumentsQuery(String type) throws SQLException {
        return connection.prepareStatement(
                "SELECT t1." + INSTRUMENT_COLUMN_ID + " , " + INSTRUMENT_COLUMN_PRICE + ", " + INSTRUMENT_COLUMN_BRAND + ", " + INSTRUMENT_COLUMN_QUALITY + " " +
                        "FROM rentable_instrument as t1 " +
                        "LEFT JOIN lease AS t2 ON t2.id = t1.lease_id " +
                        "WHERE t2.id IS NULL AND type ='" + type + "'");
    }

    /**
     * Select counts all rentals from a given student
     */
    private PreparedStatement getStudentRentalsCountQuery(int studentId) throws SQLException {
        return connection.prepareStatement(
                "SELECT COUNT(*) " +
                        "FROM student as t1 JOIN lease as t2 " +
                        "ON t1.id = t2.student_id " +
                        "WHERE t1.id = " + studentId);
    }

    /**
     * Creates a lease starting at the current date and ending at the specified end date
     **/
    private PreparedStatement getLeaseCreatorQuery(int studentId, String endDay) throws SQLException {
        return connection.prepareStatement(
                "INSERT INTO lease(student_id, start_day, end_day) " +
                        "VALUES " +
                        "(" + studentId + ", " + "CURRENT_DATE" + ", '" + endDay + "')" +
                        "RETURNING " + LEASE_COLUMN_ID);
    }

    /**
     * Adds lease to instrument
     *
     * @param instrumentId is an integer to allow for setting null, e.g. removing a rental.
     **/
    private PreparedStatement getInstrumentLeaseUpdateQuery(Integer instrumentId, int leaseId) throws SQLException {
        return connection.prepareStatement(
                "UPDATE rentable_instrument " +
                        "SET lease_id = " + leaseId + " " +
                        "WHERE " + LEASE_COLUMN_ID + " = " + instrumentId);
    }

    /**
     * Updates lease to set end_day as current day, meaning terminated
     **/
    private PreparedStatement getLeaseTerminationQuery(int leaseId) throws SQLException {
        return connection.prepareStatement(
                "UPDATE lease " +
                        "SET end_day = CURRENT_DAY " +
                        "WHERE id = " + leaseId);
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
            throw new SchoolDBException(completeFailureMsg, cause);
        } else {
            throw new SchoolDBException(completeFailureMsg);
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
