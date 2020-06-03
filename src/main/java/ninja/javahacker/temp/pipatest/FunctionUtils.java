package ninja.javahacker.temp.pipatest;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

/**
 * Just a few static functions for handling functional stuff.
 * @author Victor Williams Stafusa da Silva
 */
public class FunctionUtils {

    /**
     * This class isn't instantiable.
     */
    private FunctionUtils() {
        throw new UnsupportedOperationException("No instances.");
    }

    /**
     * If a value is present, performs the given action with the value, otherwise performs the given empty-based action.
     * <p>Java 9 features the {@code Optional.ifPresentOrElse(Consumer<? super X>, Runnable)} method, but since we are working with
     * Java 8, this method provides the functionality of that method.</p>
     * @param <X> The type of the {@link Optional}.
     * @param what The {@link Optional}
     * @param receiver The action to be performed, if a value is present.
     * @param onElse The empty-based action to be performed, if no value is present.
     * @throws NullPointerException If the value itself is {@code null} or if a value is present and the given action is {@code null},
     *     or no value is present and the given empty-based action is {@code null}.
     */
    public static <X> void ifPresentOrElse(@NonNull Optional<X> what, @NonNull Consumer<? super X> receiver, @NonNull Runnable onElse) {
        if (what.isPresent()) {
            receiver.accept(what.get());
        } else {
            onElse.run();
        }
    }

    /**
     * If a value is present, performs the given action with the value, otherwise performs the given empty-based action.
     * <p>Java 9 features the {@code OptionalLong.ifPresentOrElse(LongConsumer, Runnable)} method, but since we are working with
     * Java 8, this method provides the functionality of that method.</p>
     * @param what The {@link Optional}
     * @param receiver The action to be performed, if a value is present.
     * @param onElse The empty-based action to be performed, if no value is present.
     * @throws NullPointerException If the value itself is {@code null} or if a value is present and the given action is {@code null},
     *     or no value is present and the given empty-based action is {@code null}.
     */
    public static void ifPresentOrElse(@NonNull OptionalLong what, @NonNull LongConsumer receiver, @NonNull Runnable onElse) {
        if (what.isPresent()) {
            receiver.accept(what.getAsLong());
        } else {
            onElse.run();
        }
    }

    /**
     * Parses a {@code long} using the {@link Long#parseLong(String)} method, but returns an {@link Optional} containing the parsed value.
     * If the parse fails, an empty {@link Optional} is returned.
     * @param toParse The value to be parsed as a long.
     * @return An {@link Optional} containing the parsed value or an empty {@link Optional} if the parse fails.
     */
    @NonNull
    public static OptionalLong parseOptionalLong(@NonNull String toParse) {
        try {
            return OptionalLong.of(Long.parseLong(toParse));
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }

    /**
     * Equivalent of {@link Supplier} where the functional method might also throw any exception.
     * @param <T> The type of results supplied by this supplier.
     */
    @FunctionalInterface
    public static interface ThrowingSupplier<T> {

        /**
         * Gets a result.
         * @return A result.
         * @throws Throwable If it was not possible to get the result.
         */
        public T get() throws Throwable;
    }

    /**
     * Tries to obtain a value from the given {@link ThrowingSupplier}, and executes one action if it succeeds or the other action if it
     * does not.
     * @param <T> The type of object obtained from the {@code func} {@link ThrowingSupplier}.
     * @param func The supplier that will give out some object.
     * @param ifOk The action to be performed with the value obtained from the {@code func} {@link ThrowingSupplier}.
     * @param cons The action to be performed with the exception obtained from the failure of the execution of the {@code func}
     *     {@link ThrowingSupplier}.
     */
    public static <T> void tryRun(
            @NonNull ThrowingSupplier<T> func,
            @NonNull Consumer<? super T> ifOk,
            @NonNull Consumer<? super Throwable> cons)
    {
        T out;
        try {
            out = func.get();
        } catch (Throwable e) {
            cons.accept(e);
            return;
        }
        ifOk.accept(out);
    }
}
