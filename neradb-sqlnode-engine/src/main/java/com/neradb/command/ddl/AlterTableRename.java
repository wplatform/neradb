/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package com.neradb.command.ddl;

import com.neradb.api.ErrorCode;
import com.neradb.command.CommandInterface;
import com.neradb.dbobject.Database;
import com.neradb.dbobject.Right;
import com.neradb.dbobject.schema.Schema;
import com.neradb.dbobject.table.Table;
import com.neradb.engine.Session;
import com.neradb.message.DbException;

/**
 * This class represents the statement
 * ALTER TABLE RENAME
 */
public class AlterTableRename extends SchemaCommand {

    private boolean ifTableExists;
    private String oldTableName;
    private String newTableName;
    private boolean hidden;

    public AlterTableRename(Session session, Schema schema) {
        super(session, schema);
    }

    public void setIfTableExists(boolean b) {
        ifTableExists = b;
    }

    public void setOldTableName(String name) {
        oldTableName = name;
    }

    public void setNewTableName(String name) {
        newTableName = name;
    }

    @Override
    public int update() {
        session.commit(true);
        Database db = session.getDatabase();
        Table oldTable = getSchema().findTableOrView(session, oldTableName);
        if (oldTable == null) {
            if (ifTableExists) {
                return 0;
            }
            throw DbException.get(ErrorCode.TABLE_OR_VIEW_NOT_FOUND_1, oldTableName);
        }
        session.getUser().checkRight(oldTable, Right.ALL);
        Table t = getSchema().findTableOrView(session, newTableName);
        if (t != null && hidden && newTableName.equals(oldTable.getName())) {
            if (!t.isHidden()) {
                t.setHidden(hidden);
                oldTable.setHidden(true);
                db.updateMeta(session, oldTable);
            }
            return 0;
        }
        if (t != null || newTableName.equals(oldTable.getName())) {
            throw DbException.get(ErrorCode.TABLE_OR_VIEW_ALREADY_EXISTS_1, newTableName);
        }
        if (oldTable.isTemporary()) {
            throw DbException.getUnsupportedException("temp table");
        }
        db.renameSchemaObject(session, oldTable, newTableName);
        return 0;
    }

    @Override
    public int getType() {
        return CommandInterface.ALTER_TABLE_RENAME;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

}
