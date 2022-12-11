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

package se.kth.iv1351.sgm.model;

/**
 * An account in the bank.
 */
public class Person implements PersonDTO {
    private int id;
    private String holderName;
    private String ssn;

    /**
     * Creates an account for the specified holder with the balance zero. The account
     * number is unspecified.
     *
     * @param holderName The account holder's holderName.
     */
    public Person(String holderName) {
        this(null, holderName, 0);
    }

    /**
     * Creates an account for the specified holder with the specified balance. The
     * account number is unspecified.
     *
     * @param holderName The account holder's holderName.
     * @param balance    The initial balance.
     */
    public Person(String holderName, int balance) {
        this(null, holderName, balance);
    }

    /**
     * Creates an account for the specified holder with the specified balance and account
     * number.
     *
     * @param ssn     The person ssn.
     * @param holderName The account holder's holderName.
     * @param id    The person id.
     */
    public Person(String ssn, String holderName, int id) {
        this.ssn = ssn;
        this.holderName = holderName;
        this.id = id;
    }

    /**
     * @return The account number.
     */
    public int getPersonId() {
        return id;
    }

    /**
     * @return The balance.
     */
    public String getSSN() {
        return ssn;
    }

    /**
     * @return The holder's name.
     */
    public String getName() {
        return holderName;
    }

    /**
     * @return A string representation of all fields in this object.
     */
    @Override
    public String toString() {
        StringBuilder stringRepresentation = new StringBuilder();
        stringRepresentation.append("Person : [");
        stringRepresentation.append("id: ");
        stringRepresentation.append(this.id);
        stringRepresentation.append(", name: ");
        stringRepresentation.append(this.holderName);
        stringRepresentation.append(", ssn: ");
        stringRepresentation.append(this.ssn);
        stringRepresentation.append("]");
        return stringRepresentation.toString();
    }
}
