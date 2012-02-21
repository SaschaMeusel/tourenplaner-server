package database;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.beans.PropertyVetoException;

/**
 * @author Christoph Haag, Sascha Meusel, Niklas Schnelle, Peter Vollmer
 *
 */
public class DatabasePool {

    private String url;
    private String userName;
    private String password;
    private String driverClass;

    private ComboPooledDataSource cpds = null;

    private static DatabasePool instance = null;
    
    private DatabasePool(String url, String userName, String password, String driverClass)
            throws PropertyVetoException {

        this.url = url;
        this.userName = userName;
        this.password = password;
        this.driverClass = driverClass;

        init();
    }

    private void init() throws PropertyVetoException {
        this.cpds = new ComboPooledDataSource();
        try {
            cpds.setDriverClass(driverClass);
        } catch (PropertyVetoException e) {
            this.cpds = null;
            instance = null;
            throw e;
        }
        cpds.setJdbcUrl(url);
        cpds.setUser(userName);
        cpds.setPassword(password);
    }

    /**
     * You need to init a database pool before you can get a DatabaseManager object.
     * If you call init more than one time, it will not have any effects.
     * The DatabasePool will always keep the values, which were given within the first call.
     *
     * @param url
     *            A database driver specific url with the form
     *            <i>jdbc:subprotocol:subname</i> where <i>subname</i> is the server address with
     *            the database name (for example &quot;tourenplaner&quot;)
     *            and at the end some connection properties if needed
     *            (for example &quot;?autoReconnect=true&quot;)
     *            </br> Example:
     *            "jdbc:mysql://localhost:3306/tourenplaner?autoReconnect=true"
     * @param userName
     *            User name of the database user account
     * @param password
     *            To the user corresponding password
     * @param driverClass
     *            The driver class, for example: "com.mysql.jdbc.Driver"
     *
     * @see java.sql.DriverManager#getConnection(java.lang.String,java.lang.String, java.lang.String)
     * @throws PropertyVetoException Thrown if driverClass String for the database is not correct.
     * @return An object instance of the DatabasePool.
     */
    public synchronized static DatabasePool initDatabasePool(String url, String userName,
                                   String password, String driverClass) throws PropertyVetoException {
        if (instance == null) {
            instance = new DatabasePool(url, userName, password, driverClass);
        }
        return instance;
    }

    /**
     * Returns a DatabaseManager.
     *
     * @return A DatabaseManager
     */
    public synchronized DatabaseManager getDatabaseManager() {
        // This class is a singleton, so there should be no NullPointerException
        return new DatabaseManager(instance.cpds);
    }

    /**
     * This method will init a database pool if necessary and return a DatabaseManager object.
     * If the database pool is already initialized, the given parameters will be ignored.
     * The DatabasePool will always keep the values, which were given within the first call.
     *
     * @param url
     *            A database driver specific url with the form
     *            <i>jdbc:subprotocol:subname</i> where <i>subname</i> is the server address with
     *            the database name (for example &quot;tourenplaner&quot;)
     *            and at the end some connection properties if needed
     *            (for example &quot;?autoReconnect=true&quot;)
     *            </br> Example:
     *            "jdbc:mysql://localhost:3306/tourenplaner?autoReconnect=true"
     * @param userName
     *            User name of the database user account
     * @param password
     *            To the user corresponding password
     * @param driverClass
     *            The driver class, for example: "com.mysql.jdbc.Driver"
     * @return The corresponding DatabaseManager, but not null.
     *
     * @see java.sql.DriverManager#getConnection(java.lang.String,java.lang.String, java.lang.String)
     * @throws PropertyVetoException Thrown if driverClass String for the database is not correct.
     */
    public synchronized static DatabaseManager getDatabaseManager(String url, String userName,
                                              String password, String driverClass) throws PropertyVetoException {
        initDatabasePool(url, userName, password, driverClass);
        if (instance.cpds == null) {
            instance.init();
        }
        return instance.getDatabaseManager();
    }

}
