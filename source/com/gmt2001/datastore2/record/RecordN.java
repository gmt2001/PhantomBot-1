package com.gmt2001.datastore2.record;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.Table;
import org.jooq.conf.Settings;
import org.jooq.impl.UpdatableRecordImpl;

import com.gmt2001.datastore2.Datastore2;
import com.gmt2001.datastore2.datatype.AttachableDataType;

/**
 * Base for records. Can also be used to represent a record with a number of fields not supported by a concrete type
 *
 * @author gmt2001
 */
public abstract class RecordN <RR extends RecordN<RR>> extends UpdatableRecordImpl<RR> implements AttachableRecord {
    /**
     * Constructor
     * <p>
     * When using this constructor, {@code allowUpdatingPrimaryKeys} is set to {@code false}
     *
     * @param table the {@link Table} which stores this record
     */
    protected RecordN(Table<RR> table) {
        this(table, false);
    }

    /**
     * Constructor
     *
     * @param table the {@link Table} which stores this record
     * @param allowUpdatingPrimaryKeys {@code true} to allow an {@code UPDATE} to change the value of the primary key in a record
     */
    protected RecordN(Table<RR> table, boolean allowUpdatingPrimaryKeys) {
        super(table);

        Configuration c = Datastore2.instance().dslContext().configuration();

        if (allowUpdatingPrimaryKeys) {
            c = c.derive(new Settings().withUpdatablePrimaryKeys(allowUpdatingPrimaryKeys));
        }

        this.attach(c);
    }

    /**
     * Sets the value and, if the datatype is an {@link AttachableDataType}, calls {@link AttachableDataType#attach(org.jooq.UpdatableRecord, int)}
     *
     * @param index the 0-based index of the field to update
     * @param value the new value
     */
    protected void doSet(int index, Object value) {
        this.set(index, value);
        if (value != null && AttachableDataType.class.isAssignableFrom(value.getClass())) {
            ((AttachableDataType) value).attach(this, index);
        }
    }

    @Override
    public void doAttachments() {
        List<Object> values = this.intoList();

        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) != null && AttachableDataType.class.isAssignableFrom(values.get(i).getClass())) {
                ((AttachableDataType) values.get(i)).attach(this, i);
            }
        }
    }
}
