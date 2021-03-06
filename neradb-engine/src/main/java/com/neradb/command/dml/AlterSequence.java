/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package com.neradb.command.dml;

import com.neradb.command.CommandInterface;
import com.neradb.command.ddl.SchemaCommand;
import com.neradb.command.expression.Expression;
import com.neradb.common.DbException;
import com.neradb.common.ErrorCode;
import com.neradb.dbobject.Database;
import com.neradb.dbobject.Right;
import com.neradb.dbobject.schema.Schema;
import com.neradb.dbobject.schema.Sequence;
import com.neradb.dbobject.table.Column;
import com.neradb.dbobject.table.Table;
import com.neradb.engine.Session;

/**
 * This class represents the statement
 * ALTER SEQUENCE
 */
public class AlterSequence extends SchemaCommand {

    private boolean ifExists;
    private Table table;
    private String sequenceName;
    private Sequence sequence;
    private Expression start;
    private Expression increment;
    private Boolean cycle;
    private Expression minValue;
    private Expression maxValue;
    private Expression cacheSize;

    public AlterSequence(Session session, Schema schema) {
        super(session, schema);
    }

    public void setIfExists(boolean b) {
        ifExists = b;
    }

    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    @Override
    public boolean isTransactional() {
        return true;
    }

    public void setColumn(Column column) {
        table = column.getTable();
        sequence = column.getSequence();
        if (sequence == null && !ifExists) {
            throw DbException.get(ErrorCode.SEQUENCE_NOT_FOUND_1, column.getSQL());
        }
    }

    public void setStartWith(Expression start) {
        this.start = start;
    }

    public void setIncrement(Expression increment) {
        this.increment = increment;
    }

    public void setCycle(Boolean cycle) {
        this.cycle = cycle;
    }

    public void setMinValue(Expression minValue) {
        this.minValue = minValue;
    }

    public void setMaxValue(Expression maxValue) {
        this.maxValue = maxValue;
    }

    public void setCacheSize(Expression cacheSize) {
        this.cacheSize = cacheSize;
    }

    @Override
    public int update() {
        Database db = session.getDatabase();
        if (sequence == null) {
            sequence = getSchema().findSequence(sequenceName);
            if (sequence == null) {
                if (!ifExists) {
                    throw DbException.get(ErrorCode.SEQUENCE_NOT_FOUND_1, sequenceName);
                }
                return 0;
            }
        }
        if (table != null) {
            session.getUser().checkRight(table, Right.ALL);
        }
        if (cycle != null) {
            sequence.setCycle(cycle);
        }
        if (cacheSize != null) {
            long size = cacheSize.optimize(session).getValue(session).getLong();
            sequence.setCacheSize(size);
        }
        if (start != null || minValue != null ||
                maxValue != null || increment != null) {
            Long startValue = getLong(start);
            Long min = getLong(minValue);
            Long max = getLong(maxValue);
            Long inc = getLong(increment);
            sequence.modify(startValue, min, max, inc);
        }
        // need to use the system session, so that the update
        // can be committed immediately - not committing it
        // would keep other transactions from using the sequence
        Session sysSession = db.getSystemSession();
        synchronized (sysSession) {
            synchronized (db) {
                db.updateMeta(sysSession, sequence);
                sysSession.commit(true);
            }
        }
        return 0;
    }

    private Long getLong(Expression expr) {
        if (expr == null) {
            return null;
        }
        return expr.optimize(session).getValue(session).getLong();
    }

    @Override
    public int getType() {
        return CommandInterface.ALTER_SEQUENCE;
    }

}
