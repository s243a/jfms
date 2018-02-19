jfms
====

What is jfms
------------

jfms is a Java implementation of the Freenet Message System (FMS).
It is a standalone graphical application, independent of the reference
implementation.

Use at your own risk:
It is in an early development stage and the code quality (especially
error handling, input validation, etc.) is not suitable for production use.
The database scheme is unstable and may be incompatible with future versions

Requirements
------------

jfms requires Java 8 (or higher) with JavaFX. JavaFX is included in the JRE/JDK
provided by Oracle. OpenJDK users may need to install OpenJFX.

You also need a JDBC driver for SQLite.

Unfortunately, most distributions do not provide a JDBC driver package.

The easiest (but not safest) way to get a working JDBC driver is to download it
from <https://bitbucket.org/xerial/sqlite-jdbc>. The site provides a JDBC4
driver with precompiled native code for common platforms. Copy the downloaded
JAR file into the lib directory.

Building
--------

A buildfile for Apache Ant is provided. Run it by executing

	ant dist

Of course you may also use your favorite IDE instead of Ant.

If you get errors, make sure you are using Java 8 and JavaFX is available.
(The JDBC driver is not required for compiling, only for running jfms.)

Running
-------

You can start jfms via Ant:

	ant run

You can also launch it manually. Make sure the JDBC driver is in the classpath, e.g.,

	java -cp lib/sqlite-jdbc-<version>.jar:jfms.jar jfms.Jfms

On the first run a configuration wizard will pop up. The default settings
should work for most users.

It may take a while until you see messages. Currently, downloading the
intial set of trust lists may take more than one hour. This step should be
necessary only once.

jfms will create two files in the current directory:

- jfms.properties: configuration file
- jfms.db3: SQLite database
