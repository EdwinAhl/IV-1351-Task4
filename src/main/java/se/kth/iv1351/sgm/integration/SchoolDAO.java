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

import java.sql.*;
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
    private static final String INSTRUMENT_COLUMN_TYPE = "type";
    private static final String COLUMN_COUNT = "count";
    private static final String COLUMN_IS_EMPTY = "is_empty";
    private static final String LEASE_COLUMN_ID = "id";
    private static final String LEASE_COLUMN_STUDENT_ID = "student_id";
    private static final String LEASE_COLUMN_INSTRUMENT_ID = "instrument_id";

    private Connection connection;

    /**
     * Constructs a new DAO object connected to the bank database.
     */
    public SchoolDAO() throws SchoolDBException {
        try {
            connectToSchoolDB();
        } catch (ClassNotFoundException | SQLException exception) {
            throw new SchoolDBException("Could not connect to datasource.", exception);
        }
    }

    /**
     * @return A list of all rentable_instruments (not currently leased)
     */
    public List<Instrument> readRentableInstruments(String type) throws SchoolDBException {
        String failureMsg = "Could not list instruments.";
        List<Instrument> instruments = new ArrayList<>();
        try (ResultSet result = getFindAllRentableInstrumentsQuery(type).executeQuery()) {
            while (result.next()) {
                instruments.add(new Instrument(
                        result.getInt(INSTRUMENT_COLUMN_ID),
                        result.getInt(INSTRUMENT_COLUMN_PRICE),
                        result.getString(INSTRUMENT_COLUMN_TYPE),
                        result.getString(INSTRUMENT_COLUMN_BRAND),
                        result.getString(INSTRUMENT_COLUMN_QUALITY)));
            }
            connection.commit();
        } catch (SQLException sqlException) {
            handleException(failureMsg, sqlException);
        }
        return instruments;
    }

    /**
     * Reads number of leases a student with given student_id has
     **/
    public int readStudentLeaseCount(int studentId) throws SchoolDBException {
        String failureMsg = "Could not get student lease count.";
        int count = 0;
        try {
            getLeaseLockQuery(studentId).execute();
            PreparedStatement statement = getCountRentedInstrumentsQuery(null, studentId);
            count = getQueryRowCount(statement);
        } catch (SQLException sqlException) {
            handleException(failureMsg, sqlException);
        }
        return count;
    }

    /**
     * @return true if the instrument itself is not currently rented
     **/
    public boolean readCanRentInstrument(int instrumentId) throws SchoolDBException {
        String failureMsg = "Could not read instrument rented status.";
        boolean canRentInstrument = false;
        try (ResultSet rentedInstrumentsResults = getCountRentedInstrumentsQuery(instrumentId, null).executeQuery()) {
            rentedInstrumentsResults.next();
            canRentInstrument = rentedInstrumentsResults.getBoolean(COLUMN_IS_EMPTY);
        } catch (SQLException sqlException) {
            handleException(failureMsg, sqlException);
        }
        return canRentInstrument;
    }

    /**
     * Creates lease
     */
    public int createLease(int studentId, int instrumentId, String endDay) throws SchoolDBException {
        String failureMsg = "Could not add lease.";
        int lease_id = 0;
        try (ResultSet leaseResult = getLeaseCreatorQuery(studentId, instrumentId, endDay).executeQuery()) {
            leaseResult.next();
            lease_id = leaseResult.getInt("id");
            connection.commit();
        } catch (SQLException sqlException) {
            handleException(failureMsg, sqlException);
        }
        return lease_id;
    }

    /**
     * Terminates the lease by setting the end date to today and removing the rental from the instrument
     **/
    public void deleteLease(int leaseId) throws SchoolDBException {
        String failureMsg = "Could not terminate rental.";
        try {
            getLeaseTerminationQuery(leaseId).executeUpdate();
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

    private void connectToSchoolDB() throws ClassNotFoundException, SQLException {
        connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/sgm",
                "postgres", "post");
        connection.setAutoCommit(false);
    }

    private PreparedStatement getFindAllRentableInstrumentsQuery(String type) throws SQLException {
        return connection.prepareStatement(
                "SELECT DISTINCT " + INSTRUMENT_COLUMN_ID + ", " + INSTRUMENT_COLUMN_PRICE + ", " +
                        INSTRUMENT_COLUMN_BRAND + ", " + INSTRUMENT_COLUMN_QUALITY + ", " + INSTRUMENT_COLUMN_TYPE + " " +
                        "FROM rentable_instrument " +
                        // r.id should not be in the set of rented instrument ids
                        "WHERE id NOT IN (" +
                        "   SELECT DISTINCT instrument_id as id FROM lease " +
                        "   WHERE (CURRENT_DATE >= start_day AND CURRENT_DATE < end_day) " +
                        ")" +
                        // Type as specified, if blank then list all instruments
                        (type.isBlank() ? "" : ("AND " + INSTRUMENT_COLUMN_TYPE + " = '" + type + "'"))
        );
    }

    /**
     * Selects all rented instruments of given instrumentId and studentId
     * If any argument is null it will not be part of the query
     */
    private PreparedStatement getCountRentedInstrumentsQuery(Integer instrumentId, Integer studentId) throws SQLException {
        StringBuilder sb = new StringBuilder("SELECT COUNT(*), COUNT(*)=0 as " + COLUMN_IS_EMPTY + " FROM rentable_instrument AS r " +
                "JOIN lease AS l ON r.id=instrument_id " +
                // If current date is higher than start day and lower than end day --> Currently rented
                "WHERE (CURRENT_DATE >= l.start_day AND CURRENT_DATE < l.end_day)");
        if (instrumentId != null) {
            sb.append(" AND r.");
            sb.append(INSTRUMENT_COLUMN_ID);
            sb.append(" = ");
            sb.append(instrumentId);
        }
        if (studentId != null) {
            sb.append(" AND ");
            sb.append(LEASE_COLUMN_STUDENT_ID);
            sb.append(" = ");
            sb.append(studentId);
        }
        return connection.prepareStatement(sb.toString());
    }


    private int getQueryRowCount(PreparedStatement preparedStatement) throws SQLException {
        ResultSet countResult = preparedStatement.executeQuery();
        countResult.next();
        return countResult.getInt(COLUMN_COUNT);
    }

    /**
     * Locks leases for isolation
     */
    private PreparedStatement getLeaseLockQuery(int studentId) throws SQLException {
        return connection.prepareStatement(
                // Since the lease rows for this student should not be accessed by
                // other queries at the same time a SELECT FOR UPDATE is used.
                // If the leases were not locked then a student may be able to end up with more than allowed rentals.
                "SELECT * FROM lease WHERE student_id = '" + studentId + "' FOR UPDATE");
    }

    /**
     * Creates a lease starting at the current date and ending at the specified end date
     **/
    private PreparedStatement getLeaseCreatorQuery(int studentId, int instrumentId, String endDay) throws SQLException {
        return connection.prepareStatement(
                "INSERT INTO lease(student_id, instrument_id, start_day, end_day) " +
                        "VALUES " +
                        "(" + studentId + ", " + instrumentId + ", " + "CURRENT_DATE" + ", '" + endDay + "') " +
                        "RETURNING id");
    }

    /**
     * Updates lease to set end_day as current day, meaning terminated
     *
     * @return PreparedStatement which can generate a ResultSet containing instrument id
     **/
    private PreparedStatement getLeaseTerminationQuery(int leaseId) throws SQLException {
        return connection.prepareStatement(
                "UPDATE lease " +
                        "SET end_day = CURRENT_DATE " +
                        "WHERE id = " + leaseId
        );
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
