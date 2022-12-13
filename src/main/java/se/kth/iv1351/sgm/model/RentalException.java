package se.kth.iv1351.sgm.model;


/**
 * Thrown when create, read or delete of a rental fails.
 */
public class RentalException extends Exception {
    /**
     * Create a new instance thrown because of the specified reason.
     *
     * @param reason Why the exception was thrown.
     */
    public RentalException(String reason) {
        super(reason);
    }

    /**
     * Create a new instance thrown because of the specified reason and exception.
     *
     * @param reason    Why the exception was thrown.
     * @param rootCause The exception that caused this exception to be thrown.
     */
    public RentalException(String reason, Throwable rootCause) {
        super(reason, rootCause);
    }
}
