/*
 * Copyright (C) 2016-2023 phantombot.github.io/PhantomBot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmt2001.module.meta;

import java.util.Optional;

import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import com.gmt2001.datastore2.Datastore2;
import com.gmt2001.datastore2.datatype.AttachableJSONObject;
import com.gmt2001.datastore2.datatype.JSONObjectConverter;
import com.gmt2001.datastore2.meta.TableVersion;
import com.gmt2001.datastore2.meta.TableVersionRecord;

/**
 * Stores general settings for modules
 *
 * @author gmt2001
 */
public final class SettingsTable extends TableImpl<SettingsRecord> {

    /**
     * Instance
     */
    private static final SettingsTable INSTANCE = new SettingsTable();

    /**
     * Table name in the database
     */
    private static final String TABLENAME = Datastore2.PREFIX + "_Settings";

    /**
     * Provides an instance of {@link SettingsTable}
     *
     * @return an instance of {@link SettingsTable}
     */
    public static SettingsTable instance() {
        return INSTANCE;
    }

    static {
        checkAndCreateTable();
    }

    /**
     * The class holding records for this table
     */
    @Override
    public Class<SettingsRecord> getRecordType() {
        return SettingsRecord.class;
    }

    /**
     * The name of the module whose settings are stored in the record
     */
    public final TableField<SettingsRecord, String> MODULE = createField(DSL.name("module"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The settings as JSON
     */
    public final TableField<SettingsRecord, AttachableJSONObject> SETTINGS = createField(DSL.name("setting"), JSONObjectConverter.JSONOBJECT.nullable(false), this, "");

    /**
     * Constructor
     */
    private SettingsTable() {
        super(DSL.name(TABLENAME));
    }

    /**
     * The primary key constraint
     *
     * @return the key
     */
    @Override
    public UniqueKey<SettingsRecord> getPrimaryKey() {
        return Internal.createUniqueKey(this, DSL.name(TABLENAME + "_PK"), this.MODULE);
    }

    /**
     * Checks if the database table for {@link SettingsTable} exists, and creates it if it is missing
     */
    private static void checkAndCreateTable() {
        Optional<Table<?>> table = Datastore2.instance().findTable(TABLENAME);

        TableVersionRecord tvrecord = Datastore2.instance().dslContext().fetchOne(TableVersion.instance(), TableVersion.instance().TABLE.eq(TABLENAME));

        long version = tvrecord == null ? 0L : tvrecord.version();

        if (!table.isPresent() || version < SettingsRecord.serialVersionUID) {
            try {
                Datastore2.instance().dslContext().createTableIfNotExists(TABLENAME)
                    .column(SettingsTable.instance().MODULE)
                    .column(SettingsTable.instance().SETTINGS)
                    .primaryKey(SettingsTable.instance().MODULE).execute();

                Datastore2.instance().invalidateTableCache();
            } catch (Exception ex) {
                com.gmt2001.Console.err.printStackTrace(ex);
            }

            try {
                TableVersionRecord record = new TableVersionRecord();
                record.values(SettingsTable.instance(), SettingsRecord.serialVersionUID);
                record.store();
            } catch (Exception ex) {
                com.gmt2001.Console.err.printStackTrace(ex);
            }
        }
    }
}