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

package se.kth.iv1351.sgm.controller;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import se.kth.iv1351.sgm.integration.SchoolDAO;
import se.kth.iv1351.sgm.integration.SchoolDBException;
import se.kth.iv1351.sgm.model.InstrumentDTO;
import se.kth.iv1351.sgm.model.InstrumentStockException;
import se.kth.iv1351.sgm.model.RentalException;

/**
 * This is the application's only controller, all calls to the model pass here.
 * The controller is also responsible for calling the DAO. Typically, the
 * controller first calls the DAO to retrieve data (if needed), then operates on
 * the data, and finally tells the DAO to store the updated data (if any).
 */
public class Controller {
    private final SchoolDAO schoolDb;

    /**
     * Creates a new instance, and retrieves a connection to the database.
     *
     * @throws SchoolDBException If unable to connect to the database.
     */
    public Controller() throws SchoolDBException {
        schoolDb = new SchoolDAO();
    }


    /**
     * Lists all rentable_instruments
     **/
    public List<? extends InstrumentDTO> getInstruments(String type) throws InstrumentStockException {
        try {
            return schoolDb.readRentableInstruments(type);
        } catch (Exception e) {
            throw new InstrumentStockException("Unable to list instruments.", e);
        }
    }

    /**
     * Adds lease
     **/
    public void createLease(int studentId, int instrumentId, String endDay) throws RentalException {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date parsedDate = sdf.parse(endDay);

            Calendar cal = Calendar.getInstance();
            Date currentDate = cal.getTime();
            cal.add(Calendar.YEAR, 1);
            Date currentTimePlusOneYear = cal.getTime();

            if (parsedDate.after(currentTimePlusOneYear) || parsedDate.before(currentDate)) {
                throw new RentalException("The end rent date cannot be today or after more than 12 months.");
            }

            int countResult = schoolDb.readStudentLeaseCount(studentId);
            if (countResult >= 2) {
                throw new RentalException("Student cannot have more than 2 rentals simultaneously.");
            }

            boolean canRentInstrument = schoolDb.readCanRentInstrument(instrumentId);
            if (!canRentInstrument) {
                throw new RentalException("Instrument cannot be rented");
            }
            schoolDb.createLease(studentId, instrumentId, endDay);
        } catch (Exception e) {
            throw new RentalException("Unable to rent.", e);
        }
    }

    /**
     * Terminates lease
     **/
    public void terminateLease(int leaseId) throws RentalException {
        try {
            schoolDb.deleteLease(leaseId);
            System.out.println("Terminated leaseId " + leaseId);
        } catch (Exception e) {
            throw new RentalException("Unable to terminate lease.", e);
        }
    }
}
