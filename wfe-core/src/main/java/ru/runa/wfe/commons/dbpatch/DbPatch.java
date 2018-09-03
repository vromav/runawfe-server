package ru.runa.wfe.commons.dbpatch;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.sql.Types;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.springframework.beans.factory.annotation.Autowired;
import ru.runa.wfe.InternalApplicationException;
import ru.runa.wfe.commons.ApplicationContextFactory;
import ru.runa.wfe.commons.DbType;

/**
 * Interface for database patch (Applied during version update).
 * 
 * @author Dofs
 */
public abstract class DbPatch {
    protected Log log = LogFactory.getLog(getClass());
    protected final Dialect dialect = ApplicationContextFactory.getDialect();
    protected final DbType dbType = ApplicationContextFactory.getDBType();
    @Autowired
    protected SessionFactory sessionFactory;

    public void execute() throws Exception {
        Session session = sessionFactory.getCurrentSession();
        executeDDL(session, "[DDLBefore]", getDDLQueriesBefore());
        session.setCacheMode(CacheMode.IGNORE);
        executeDML(session);
        session.flush();
        executeDDL(session, "[DDLAfter]", getDDLQueriesAfter());
    }

    protected List<String> getDDLQueriesBefore() {
        return Lists.newArrayList();
    }

    /**
     * Execute patch DML statements in one transaction.
     * 
     * It's allowed to use only raw SQL because hibernate mappings could not work in old DB version.
     * 
     * This is preferable way to patch database (@see {@link DbPatchPostProcessor}).
     */
    public void executeDML(Session session) throws Exception {

    }

    protected List<String> getDDLQueriesAfter() {
        return Lists.newArrayList();
    }

    private void executeDDL(Session session, String category, List<String> queries) throws Exception {
        for (String query : queries) {
            if (!Strings.isNullOrEmpty(query)) {
                log.info(category + ": " + query);
                session.createSQLQuery(query).executeUpdate();
            }
        }
    }

    protected final String getDDLCreateSequence(String sequenceName) {
        if (dbType == DbType.ORACLE || dbType == DbType.POSTGRESQL) {
            return "CREATE SEQUENCE " + sequenceName;
        }
        return null;
    }

    protected final String getDDLCreateTable(String tableName, List<ColumnDef> columnDefinitions, String unique) {
        String query = "CREATE TABLE " + tableName + " (";
        for (ColumnDef columnDef : columnDefinitions) {
            if (columnDefinitions.indexOf(columnDef) > 0) {
                query += ", ";
            }
            query += columnDef.name + " " + columnDef.getSqlTypeName(dialect);
            if (columnDef.primaryKey) {
                String primaryKeyModifier;
                switch (dbType) {
                case HSQL:
                case MSSQL:
                    primaryKeyModifier = "IDENTITY NOT NULL PRIMARY KEY";
                    break;
                case ORACLE:
                    primaryKeyModifier = "NOT NULL PRIMARY KEY";
                    break;
                case POSTGRESQL:
                    primaryKeyModifier = "PRIMARY KEY";
                    break;
                case MYSQL:
                    primaryKeyModifier = "NOT NULL PRIMARY KEY AUTO_INCREMENT";
                    break;
                case H2:
                    primaryKeyModifier = "GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY";
                    break;
                default:
                    primaryKeyModifier = "PRIMARY KEY";
                    break;
                }
                query += " " + primaryKeyModifier;
                continue;
            }
            if (!columnDef.allowNulls) {
                query += " NOT NULL";
            }
        }
        if (unique != null) {
            query += ", UNIQUE " + unique;
        }
        query += ")";
        return query;
    }

    protected final String getDDLRenameTable(String oldTableName, String newTableName) {
        String query;
        switch (dbType) {
        case MSSQL:
            query = "sp_rename '" + oldTableName + "', '" + newTableName + "'";
            break;
        case MYSQL:
            query = "RENAME TABLE " + oldTableName + " TO " + newTableName;
            break;
        default:
            query = "ALTER TABLE " + oldTableName + " RENAME TO " + newTableName;
            break;
        }
        return query;
    }

    protected final String getDDLRemoveTable(String tableName) {
        return "DROP TABLE " + tableName;
    }

    protected final String getDDLCreateIndex(String tableName, String indexName, String... columnNames) {
        String conjunctedColumnNames = Joiner.on(", ").join(columnNames);
        return "CREATE INDEX " + indexName + " ON " + tableName + " (" + conjunctedColumnNames + ")";
    }

    protected final String getDDLCreateUniqueKey(String tableName, String indexName, String... columnNames) {
        String conjunctedColumnNames = Joiner.on(", ").join(columnNames);
        return "ALTER TABLE " + tableName + " ADD CONSTRAINT " + indexName + " UNIQUE (" + conjunctedColumnNames + ")";
    }

    protected final String getDDLRenameIndex(String tableName, String indexName, String newIndexName) {
        String query;
        switch (dbType) {
        case MSSQL:
            query = "sp_rename '" + tableName + "." + indexName + "', '" + newIndexName + "'";
            break;
        default:
            throw new InternalApplicationException("TODO");
        }
        return query;
    }

    protected final String getDDLRemoveIndex(String tableName, String indexName) {
        switch (dbType) {
        case H2:
        case ORACLE:
        case POSTGRESQL:
            return "DROP INDEX " + indexName;
        default:
            return "DROP INDEX " + indexName + " ON " + tableName;
        }
    }

    protected final String getDDLCreateForeignKey(String tableName, String keyName, String columnName, String refTableName, String refColumnName) {
        return "ALTER TABLE " + tableName + " ADD CONSTRAINT " + keyName + " FOREIGN KEY (" + columnName + ") REFERENCES " + refTableName + " ("
                + refColumnName + ")";
    }

    protected final String getDDLCreatePrimaryKey(String tableName, String keyName, String columnName) {
        return "ALTER TABLE " + tableName + " ADD CONSTRAINT " + keyName + " PRIMARY KEY (" + columnName + ")";
    }

    protected final String getDDLRenameForeignKey(String keyName, String newKeyName) {
        String query;
        switch (dbType) {
        case MSSQL:
            query = "sp_rename '" + keyName + "', '" + newKeyName + "'";
            break;
        default:
            throw new InternalApplicationException("TODO");
        }
        return query;
    }

    protected final String getDDLRemoveForeignKey(String tableName, String keyName) {
        String constraint;
        switch (dbType) {
        case MYSQL:
            constraint = "FOREIGN KEY";
            break;
        default:
            constraint = "CONSTRAINT";
            break;
        }
        return "ALTER TABLE " + tableName + " DROP " + constraint + " " + keyName;
    }

    protected final String getDDLCreateColumn(String tableName, ColumnDef columnDef) {
        String lBraced = "";
        String rBraced = "";
        if (dbType == DbType.ORACLE) {
            lBraced = "(";
            rBraced = ")";
        }
        String query = "ALTER TABLE " + tableName + " ADD " + lBraced;
        query += columnDef.name + " " + columnDef.getSqlTypeName(dialect);
        if (columnDef.defaultValue != null) {
            query += " DEFAULT " + columnDef.defaultValue;
        }
        if (!columnDef.allowNulls) {
            query += " NOT NULL";
        }
        query += rBraced;
        return query;
    }

    protected final String getDDLRenameColumn(String tableName, String oldColumnName, ColumnDef newColumnDef) {
        String query;
        switch (dbType) {
        case ORACLE:
        case POSTGRESQL:
            query = "ALTER TABLE " + tableName + " RENAME COLUMN " + oldColumnName + " TO " + newColumnDef.name;
            break;
        case MSSQL:
            query = "sp_rename '" + tableName + "." + oldColumnName + "', '" + newColumnDef.name + "', 'COLUMN'";
            break;
        case MYSQL:
            query = "ALTER TABLE " + tableName + " CHANGE " + oldColumnName + " " + newColumnDef.name + " " + newColumnDef.getSqlTypeName(dialect);
            break;
        default:
            query = "ALTER TABLE " + tableName + " ALTER COLUMN " + oldColumnName + " RENAME TO " + newColumnDef.name;
            break;
        }
        return query;
    }

    protected final String getDDLModifyColumn(String tableName, String columnName, String sqlTypeName) {
        String query;
        switch (dbType) {
        case ORACLE:
            query = "ALTER TABLE " + tableName + " MODIFY(" + columnName + " " + sqlTypeName + ")";
            break;
        case POSTGRESQL:
            query = "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " TYPE " + sqlTypeName;
            break;
        case MYSQL:
            query = "ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + sqlTypeName;
            break;
        default:
            query = "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " " + sqlTypeName;
            break;
        }
        return query;
    }

    protected final String getDDLModifyColumnNullability(String tableName, String columnName, String currentSqlTypeName, boolean nullable) {
        String query;
        switch (dbType) {
        case ORACLE:
            query = "ALTER TABLE " + tableName + " MODIFY(" + columnName + " " + (nullable ? "NULL" : "NOT NULL") + ")";
            break;
        case POSTGRESQL:
            query = "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " " + (nullable ? "DROP" : "SET") + " NOT NULL";
            break;
        case H2:
        case HSQL:
            query = "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " SET " + (nullable ? "NULL" : "NOT NULL");
            break;
        case MYSQL:
            query = "ALTER TABLE " + tableName + " MODIFY " + columnName + " " + currentSqlTypeName + " " + (nullable ? "NULL" : "NOT NULL");
            break;
        default:
            query = "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " " + currentSqlTypeName + " " + (nullable ? "NULL" : "NOT NULL");
            break;
        }
        return query;
    }

    protected final String getDDLRemoveColumn(String tableName, String columnName) {
        return "ALTER TABLE " + tableName + " DROP COLUMN " + columnName;
    }

    protected final String getDDLTruncateTable(String tableName) {
        return "TRUNCATE TABLE " + tableName;
    }

    protected final String getDDLTruncateTableUsingDelete(String tableName) {
        return "DELETE FROM " + tableName;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public static class ColumnDef {
        private boolean primaryKey;
        private final String name;
        private int sqlType;
        private String sqlTypeName;
        private final boolean allowNulls;
        private String defaultValue;

        public ColumnDef(String name, int sqlType, boolean allowNulls) {
            this.name = name;
            this.sqlType = sqlType;
            this.allowNulls = allowNulls;
        }

        public ColumnDef(String name, String sqlTypeName, boolean allowNulls) {
            this.name = name;
            this.sqlTypeName = sqlTypeName;
            this.allowNulls = allowNulls;
        }

        /**
         * Creates column def which allows null values.
         */
        public ColumnDef(String name, int sqlType) {
            this(name, sqlType, true);
        }

        /**
         * Creates column def which allows null values.
         */
        public ColumnDef(String name, String sqlTypeName) {
            this(name, sqlTypeName, true);
        }

        public String getSqlTypeName(Dialect dialect) {
            if (sqlTypeName != null) {
                return sqlTypeName;
            }
            return dialect.getTypeName(sqlType);
        }

        public ColumnDef setPrimaryKey() {
            primaryKey = true;
            return this;
        }

        public ColumnDef setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }
    }

    public class BigintColumnDef extends ColumnDef {
        public BigintColumnDef(String name, boolean allowNulls) {
            super(name, Types.BIGINT, allowNulls);
        }
    }

    public class BooleanColumnDef extends ColumnDef {
        public BooleanColumnDef(String name, boolean allowNulls) {
            super(name, Types.TINYINT, allowNulls);
        }
    }

    public class IntColumnDef extends ColumnDef {
        public IntColumnDef(String name, boolean allowNulls) {
            super(name, Types.INTEGER, allowNulls);
        }
    }

    public class VarcharColumnDef extends ColumnDef {
        public VarcharColumnDef(String name, int length, boolean allowNulls) {
            // Don't know why length is passed 3 times here (as length, precision and scale),
            // but I didn't like it and thus made this helper class.
            // Other helper classes (BigintColumnDef, IntColumnDef) are just for company.
            super(name, dialect.getTypeName(Types.VARCHAR, length, length, length), allowNulls);
        }
    }
}