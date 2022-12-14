# A JDBC application with an appropriately layered architecture

This is an example of how an integration layer can be used to organize an application containing database calls.

## Commands for the school program

* `help` displays all commands.
* `list` lists all rentable instruments.
* `list <instrument type>` lists all rentble instruments of specified type.
* `rent <student id> <instrument id> <end_day>` creates a lease for student with instrument that ends on specified day
* `terminate <lease id>` sets leases end day to current day, to indicate it has expired 
* `quit` quits the application.
