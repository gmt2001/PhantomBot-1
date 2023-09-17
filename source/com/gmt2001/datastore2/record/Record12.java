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
package com.gmt2001.datastore2.record;

import java.util.function.Supplier;

import org.jooq.Field;
import org.jooq.Row12;
import org.jooq.Table;

/**
 * Abstract class which simplifies setup and usage of {@link org.jooq.Record12} on an {@link UpdateableRecordImpl}
 *
 * @param <RR> self-reference to the implementing class
 * @param <A> the Java data type of field 1, which is also the primary key
 * @param <B> the Java data type of field 2
 * @param <C> the Java data type of field 3
 * @param <D> the Java data type of field 4
 * @param <E> the Java data type of field 5
 * @param <F> the Java data type of field 6
 * @param <G> the Java data type of field 7
 * @param <H> the Java data type of field 8
 * @param <I> the Java data type of field 9
 * @param <J> the Java data type of field 10
 * @param <K> the Java data type of field 11
 * @param <L> the Java data type of field 12
 *
 * @author gmt2001
 */
public abstract class Record12 <RR extends Record12<RR, A, B, C, D, E, F, G, H, I, J, K, L>, A, B, C, D, E, F, G, H, I, J, K, L>
    extends RecordN<RR> implements org.jooq.Record12<A, B, C, D, E, F, G, H, I, J, K, L> {
    /**
     * The {@link Supplier} for the {@code A} {@link Field}, which is also the primary key
     */
    private final Supplier<Field<A>> field1Supplier;
    /**
     * The {@link Supplier} for the {@code B} {@link Field}
     */
    private final Supplier<Field<B>> field2Supplier;
    /**
     * The {@link Supplier} for the {@code C} {@link Field}
     */
    private final Supplier<Field<C>> field3Supplier;
    /**
     * The {@link Supplier} for the {@code D} {@link Field}
     */
    private final Supplier<Field<D>> field4Supplier;
    /**
     * The {@link Supplier} for the {@code E} {@link Field}
     */
    private final Supplier<Field<E>> field5Supplier;
    /**
     * The {@link Supplier} for the {@code F} {@link Field}
     */
    private final Supplier<Field<F>> field6Supplier;
    /**
     * The {@link Supplier} for the {@code G} {@link Field}
     */
    private final Supplier<Field<G>> field7Supplier;
    /**
     * The {@link Supplier} for the {@code H} {@link Field}
     */
    private final Supplier<Field<H>> field8Supplier;
    /**
     * The {@link Supplier} for the {@code I} {@link Field}
     */
    private final Supplier<Field<I>> field9Supplier;
    /**
     * The {@link Supplier} for the {@code J} {@link Field}
     */
    private final Supplier<Field<J>> field10Supplier;
    /**
     * The {@link Supplier} for the {@code K} {@link Field}
     */
    private final Supplier<Field<K>> field11Supplier;
    /**
     * The {@link Supplier} for the {@code L} {@link Field}
     */
    private final Supplier<Field<L>> field12Supplier;

    /**
     * Constructor
     * <p>
     * When using this constructor, {@code allowUpdatingPrimaryKeys} is set to {@code false}
     *
     * @param table the {@link Table} which stores this record
     * @param field1Supplier the {@link Supplier} for the {@code A} {@link Field}, which is also the primary key
     * @param field2Supplier the {@link Supplier} for the {@code B} {@link Field}
     * @param field3Supplier the {@link Supplier} for the {@code C} {@link Field}
     * @param field4Supplier the {@link Supplier} for the {@code D} {@link Field}
     * @param field5Supplier the {@link Supplier} for the {@code E} {@link Field}
     * @param field6Supplier the {@link Supplier} for the {@code F} {@link Field}
     * @param field7Supplier the {@link Supplier} for the {@code G} {@link Field}
     * @param field8Supplier the {@link Supplier} for the {@code H} {@link Field}
     * @param field9Supplier the {@link Supplier} for the {@code I} {@link Field}
     * @param field10Supplier the {@link Supplier} for the {@code J} {@link Field}
     * @param field11Supplier the {@link Supplier} for the {@code K} {@link Field}
     * @param field12Supplier the {@link Supplier} for the {@code L} {@link Field}
     */
    protected Record12(Table<RR> table, Supplier<Field<A>> field1Supplier, Supplier<Field<B>> field2Supplier,
        Supplier<Field<C>> field3Supplier, Supplier<Field<D>> field4Supplier, Supplier<Field<E>> field5Supplier,
        Supplier<Field<F>> field6Supplier, Supplier<Field<G>> field7Supplier, Supplier<Field<H>> field8Supplier,
        Supplier<Field<I>> field9Supplier, Supplier<Field<J>> field10Supplier, Supplier<Field<K>> field11Supplier,
        Supplier<Field<L>> field12Supplier) {
        this(table, false, field1Supplier, field2Supplier, field3Supplier, field4Supplier, field5Supplier,
            field6Supplier, field7Supplier, field8Supplier, field9Supplier, field10Supplier, field11Supplier, field12Supplier);
    }

    /**
     * Constructor
     *
     * @param table the {@link Table} which stores this record
     * @param allowUpdatingPrimaryKeys {@code true} to allow an {@code UPDATE} to change the value of field {@code A} in a record
     * @param field1Supplier the {@link Supplier} for the {@code A} {@link Field}, which is also the primary key
     * @param field2Supplier the {@link Supplier} for the {@code B} {@link Field}
     * @param field3Supplier the {@link Supplier} for the {@code C} {@link Field}
     * @param field4Supplier the {@link Supplier} for the {@code D} {@link Field}
     * @param field5Supplier the {@link Supplier} for the {@code E} {@link Field}
     * @param field6Supplier the {@link Supplier} for the {@code F} {@link Field}
     * @param field7Supplier the {@link Supplier} for the {@code G} {@link Field}
     * @param field8Supplier the {@link Supplier} for the {@code H} {@link Field}
     * @param field9Supplier the {@link Supplier} for the {@code I} {@link Field}
     * @param field10Supplier the {@link Supplier} for the {@code J} {@link Field}
     * @param field11Supplier the {@link Supplier} for the {@code K} {@link Field}
     * @param field12Supplier the {@link Supplier} for the {@code L} {@link Field}
     */
    protected Record12(Table<RR> table, boolean allowUpdatingPrimaryKeys, Supplier<Field<A>> field1Supplier, Supplier<Field<B>> field2Supplier,
        Supplier<Field<C>> field3Supplier, Supplier<Field<D>> field4Supplier, Supplier<Field<E>> field5Supplier,
        Supplier<Field<F>> field6Supplier, Supplier<Field<G>> field7Supplier, Supplier<Field<H>> field8Supplier,
        Supplier<Field<I>> field9Supplier, Supplier<Field<J>> field10Supplier, Supplier<Field<K>> field11Supplier,
        Supplier<Field<L>> field12Supplier) {
        super(table, allowUpdatingPrimaryKeys);
        this.field1Supplier = field1Supplier;
        this.field2Supplier = field2Supplier;
        this.field3Supplier = field3Supplier;
        this.field4Supplier = field4Supplier;
        this.field5Supplier = field5Supplier;
        this.field6Supplier = field6Supplier;
        this.field7Supplier = field7Supplier;
        this.field8Supplier = field8Supplier;
        this.field9Supplier = field9Supplier;
        this.field10Supplier = field10Supplier;
        this.field11Supplier = field11Supplier;
        this.field12Supplier = field12Supplier;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public org.jooq.Record1<A> key() {
        return (org.jooq.Record1) super.key();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Row12<A, B, C, D, E, F, G, H, I, J, K, L> fieldsRow() {
        return (Row12) super.fieldsRow();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Row12<A, B, C, D, E, F, G, H, I, J, K, L> valuesRow() {
        return (Row12) super.valuesRow();
    }

    @Override
    public Field<A> field1() {
        return field1Supplier.get();
    }

    @Override
    public Field<B> field2() {
        return field2Supplier.get();
    }

    @Override
    public Field<C> field3() {
        return field3Supplier.get();
    }

    @Override
    public Field<D> field4() {
        return field4Supplier.get();
    }

    @Override
    public Field<E> field5() {
        return field5Supplier.get();
    }

    @Override
    public Field<F> field6() {
        return field6Supplier.get();
    }

    @Override
    public Field<G> field7() {
        return field7Supplier.get();
    }

    @Override
    public Field<H> field8() {
        return field8Supplier.get();
    }

    @Override
    public Field<I> field9() {
        return field9Supplier.get();
    }

    @Override
    public Field<J> field10() {
        return field10Supplier.get();
    }

    @Override
    public Field<K> field11() {
        return field11Supplier.get();
    }

    @Override
    public Field<L> field12() {
        return field12Supplier.get();
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public A value1() {
        return (A) this.get(0);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public B value2() {
        return (B) this.get(1);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public C value3() {
        return (C) this.get(2);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public D value4() {
        return (D) this.get(3);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public E value5() {
        return (E) this.get(4);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public F value6() {
        return (F) this.get(5);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public G value7() {
        return (G) this.get(6);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public H value8() {
        return (H) this.get(7);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public I value9() {
        return (I) this.get(8);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public J value10() {
        return (J) this.get(9);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public K value11() {
        return (K) this.get(10);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public L value12() {
        return (L) this.get(11);
    }

    @Override
    public org.jooq.Record12<A, B, C, D, E, F, G, H, I, J, K, L> value1(A value) {
        this.doSet(0, value);
        return this;
    }

    @Override
    public org.jooq.Record12<A, B, C, D, E, F, G, H, I, J, K, L> value2(B value) {
        this.doSet(1, value);
        return this;
    }

    @Override
    public org.jooq.Record12<A, B, C, D, E, F, G, H, I, J, K, L> value3(C value) {
        this.doSet(2, value);
        return this;
    }

    @Override
    public org.jooq.Record12<A, B, C, D, E, F, G, H, I, J, K, L> value4(D value) {
        this.doSet(3, value);
        return this;
    }

    @Override
    public org.jooq.Record12<A, B, C, D, E, F, G, H, I, J, K, L> value5(E value) {
        this.doSet(4, value);
        return this;
    }

    @Override
    public org.jooq.Record12<A, B, C, D, E, F, G, H, I, J, K, L> value6(F value) {
        this.doSet(5, value);
        return this;
    }

    @Override
    public org.jooq.Record12<A, B, C, D, E, F, G, H, I, J, K, L> value7(G value) {
        this.doSet(6, value);
        return this;
    }

    @Override
    public org.jooq.Record12<A, B, C, D, E, F, G, H, I, J, K, L> value8(H value) {
        this.doSet(7, value);
        return this;
    }

    @Override
    public org.jooq.Record12<A, B, C, D, E, F, G, H, I, J, K, L> value9(I value) {
        this.doSet(8, value);
        return this;
    }

    @Override
    public org.jooq.Record12<A, B, C, D, E, F, G, H, I, J, K, L> value10(J value) {
        this.doSet(9, value);
        return this;
    }

    @Override
    public org.jooq.Record12<A, B, C, D, E, F, G, H, I, J, K, L> value11(K value) {
        this.doSet(10, value);
        return this;
    }

    @Override
    public org.jooq.Record12<A, B, C, D, E, F, G, H, I, J, K, L> value12(L value) {
        this.doSet(11, value);
        return this;
    }

    @Override
    public org.jooq.Record12<A, B, C, D, E, F, G, H, I, J, K, L> values(A t1, B t2, C t3, D t4, E t5, F t6, G t7, H t8, I t9, J t10,
        K t11, L t12) {
        return this.value1(t1).value2(t2).value3(t3).value4(t4).value5(t5).value6(t6)
            .value7(t7).value8(t8).value9(t9).value10(t10).value11(t11).value12(t12);
    }

    @Override
    public A component1() {
        return this.value1();
    }

    @Override
    public B component2() {
        return this.value2();
    }

    @Override
    public C component3() {
        return this.value3();
    }

    @Override
    public D component4() {
        return this.value4();
    }

    @Override
    public E component5() {
        return this.value5();
    }

    @Override
    public F component6() {
        return this.value6();
    }

    @Override
    public G component7() {
        return this.value7();
    }

    @Override
    public H component8() {
        return this.value8();
    }

    @Override
    public I component9() {
        return this.value9();
    }

    @Override
    public J component10() {
        return this.value10();
    }

    @Override
    public K component11() {
        return this.value11();
    }

    @Override
    public L component12() {
        return this.value12();
    }
}
