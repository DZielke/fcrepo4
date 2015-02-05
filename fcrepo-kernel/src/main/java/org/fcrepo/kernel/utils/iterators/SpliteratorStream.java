/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.kernel.utils.iterators;

import static java.util.Spliterators.spliteratorUnknownSize;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterators;

/**
 * A {@link Stream} backed by an {@link Spliterator}.
 *
 * @author ajs6f
 */
public abstract class SpliteratorStream<T, SelfType extends SpliteratorStream<T, SelfType>> implements Stream<T> {

    private Spliterator<? extends T> spliterator;

    private Spliterator<? extends T> delegate() {
        return spliterator;
    }

    protected Stream<? extends T> stream() {
        return StreamSupport.stream(delegate(), false);
    }

    @SuppressWarnings("unchecked")
    private Stream<T> upcastStream() {
        return (Stream<T>) stream();
    }

    protected SpliteratorStream(final Spliterator<? extends T> del) {
        this.spliterator = del;
    }

    protected SpliteratorStream(final Iterator<? extends T> del) {
        this.spliterator = spliteratorUnknownSize(del, 0);
    }

    protected SpliteratorStream(final Stream<? extends T> del) {
        this.spliterator = del.spliterator();
    }

    /**
     * Returns a new {@link SelfType} with proffered elements and the context of this object.
     *
     * @param elements
     * @return a new SelfType with the context of this object
     */
    public <S extends T> SelfType withThisContext(@SuppressWarnings("unchecked") final S... elements) {
        return withThisContext(Iterators.forArray(elements));
    }

    /**
     * Returns a new {@link SelfType} with proffered elements and the context of this object.
     *
     * @param elements
     * @return a new SelfType with the context of this object
     */
    public abstract SelfType withThisContext(final Spliterator<? extends T> elements);

    /**
     * Returns a new {@link SelfType} with proffered elements and the context of this object.
     *
     * @param elements
     * @return a new SelfType with the context of this object
     */
    public SelfType withThisContext(final Iterator<? extends T> elements) {
        return withThisContext(spliteratorUnknownSize(elements, 0));
    }

    /**
     * Returns a new {@link SelfType} with proffered elements and the context of this object.
     *
     * @param elements
     * @return a new SelfType with the context of this object
     */
    public SelfType withThisContext(final Stream<? extends T> elements) {
        return withThisContext(elements.iterator());
    }

    /**
     * Returns a new {@link SelfType} with proffered elements and the context of this object.
     *
     * @param elements
     * @return a new SelfType with the context of this object
     */
    public SelfType withThisContext(final Iterable<? extends T> elements) {
        return withThisContext(elements.iterator());
    }

    /**
     * @param newElements new elements to add.
     * @return A new Stream with old followed by new elements
     */
    public SelfType concat(final Spliterator<? extends T> newElements) {
        final Stream<? extends T> concatStream = Stream.concat(stream(), StreamSupport.stream(newElements, false));
        spliterator = concatStream.spliterator();
        return withThisContext(delegate());
    }

    /**
     * @param newElements new elements to add.
     * @return A new Stream with old followed by new elements
     */
    public SelfType concat(final Stream<? extends T> newElements) {
        return concat(newElements.iterator());
    }

    /**
     * @param newElements new elements to add.
     * @return A new Stream with old followed by new elements
     */
    public SelfType concat(final Iterator<? extends T> newElements) {
        return concat(spliteratorUnknownSize(newElements, 0));
    }

    /**
     * @param newElements new elements to add.
     * @return A new Stream with old followed by new elements
     */
    public <S extends T> SelfType concat(@SuppressWarnings("unchecked") final S... newElements) {
        return concat(Iterators.forArray(newElements));
    }

    /**
     * @param newElements new elements to add.
     * @return A new Stream with old followed by new elements
     */
    public SelfType concat(final Collection<? extends T> newElements) {
        return concat(newElements.stream().spliterator());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<T> iterator() {
        return (Iterator<T>) Spliterators.iterator(delegate());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Spliterator<T> spliterator() {
        return (Spliterator<T>) spliterator;
    }

    @Override
    public boolean isParallel() {
        return false;
    }

    @Override
    public SelfType sequential() {
        return withThisContext(delegate());
    }

    @Override
    public SelfType parallel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SelfType unordered() {
        return withThisContext(delegate());
    }

    @Override
    public SelfType onClose(final Runnable closeHandler) {
        return withThisContext(stream().onClose(closeHandler));
    }

    @Override
    public void close() {
        stream().close();
    }

    @Override
    public SelfType filter(final Predicate<? super T> p) {
        return withThisContext(upcastStream().filter(p));
    }

    @Override
    public <R> Stream<R> map(final Function<? super T, ? extends R> f) {
        return upcastStream().map(f);
    }

    @Override
    public IntStream mapToInt(final ToIntFunction<? super T> f) {
        return upcastStream().mapToInt(f);
    }

    @Override
    public LongStream mapToLong(final ToLongFunction<? super T> f) {
        return upcastStream().mapToLong(f);
    }

    @Override
    public DoubleStream mapToDouble(final ToDoubleFunction<? super T> f) {
        return upcastStream().mapToDouble(f);
    }

    @Override
    public <R> Stream<R> flatMap(final Function<? super T, ? extends Stream<? extends R>> f) {
        return upcastStream().flatMap(f);
    }

    @Override
    public IntStream flatMapToInt(final Function<? super T, ? extends IntStream> f) {
        return upcastStream().flatMapToInt(f);
    }

    @Override
    public LongStream flatMapToLong(final Function<? super T, ? extends LongStream> f) {
        return upcastStream().flatMapToLong(f);
    }

    @Override
    public DoubleStream flatMapToDouble(final Function<? super T, ? extends DoubleStream> f) {
        return upcastStream().flatMapToDouble(f);
    }

    @Override
    public SelfType distinct() {
        return withThisContext(stream().distinct());
    }

    @Override
    public SelfType sorted() {
        return withThisContext(stream().sorted());
    }

    @Override
    public SelfType sorted(final Comparator<? super T> comparator) {
        return withThisContext(upcastStream().sorted(comparator));
    }

    @Override
    public SelfType peek(final Consumer<? super T> action) {
        return withThisContext(upcastStream().peek(action));
    }

    @Override
    public SelfType limit(final long maxSize) {
        return withThisContext(stream().limit(maxSize));
    }

    @Override
    public SelfType skip(final long n) {
        return withThisContext(stream().skip(n));
    }

    @Override
    public void forEach(final Consumer<? super T> action) {
        upcastStream().forEach(action);
    }

    @Override
    public void forEachOrdered(final Consumer<? super T> action) {
        upcastStream().forEachOrdered(action);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T[] toArray() {
        return (T[]) stream().toArray();
    }

    @Override
    public <A> A[] toArray(final IntFunction<A[]> generator) {
        return stream().toArray(generator);
    }

    @Override
    public T reduce(final T identity, final BinaryOperator<T> accumulator) {
        return upcastStream().reduce(identity, accumulator);
    }

    @Override
    public Optional<T> reduce(final BinaryOperator<T> accumulator) {
        return upcastStream().reduce(accumulator);
    }

    @Override
    public <U> U reduce(final U identity, final BiFunction<U, ? super T, U> accumulator,
            final BinaryOperator<U> combiner) {
        return upcastStream().reduce(identity, accumulator, combiner);
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final BiConsumer<R, ? super T> accumulator,
            final BiConsumer<R, R> combiner) {
        return upcastStream().collect(supplier, accumulator, combiner);
    }

    @Override
    public <R, A> R collect(final Collector<? super T, A, R> collector) {
        return upcastStream().collect(collector);
    }

    @Override
    public Optional<T> min(final Comparator<? super T> comparator) {
        return upcastStream().min(comparator);
    }

    @Override
    public Optional<T> max(final Comparator<? super T> comparator) {
        return upcastStream().max(comparator);
    }

    @Override
    public long count() {
        return stream().count();
    }

    @Override
    public boolean anyMatch(final Predicate<? super T> p) {
        return upcastStream().anyMatch(p);
    }

    @Override
    public boolean allMatch(final Predicate<? super T> p) {
        return upcastStream().allMatch(p);
    }

    @Override
    public boolean noneMatch(final Predicate<? super T> p) {
        return upcastStream().noneMatch(p);
    }

    @Override
    public Optional<T> findFirst() {
        return upcastStream().findFirst();
    }

    @Override
    public Optional<T> findAny() {
        return upcastStream().findAny();
    }

}
