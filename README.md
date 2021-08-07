# Cortana

The Cortana code was extracted from the group repository of Arno Knobbe to retain the modification history and previous contributors.

The build process was adapted to use maven, and the libraries were updated.

The code is compatible with Java 15. 

## To build
To package the code to a jar:
```bash
mvn package
```

## To run
```bash
java -jar target/cortana-1.x.x.jar
```

## To test
The tests are not run by default. To run the tests use
```bash
mvn test -DskipTests
```

## TODO

* Set version number
* Add licence
* Set javadoc plugin in maven
* Extend testing
* Update code to remove deprecated functions/classes
* Remove death code
