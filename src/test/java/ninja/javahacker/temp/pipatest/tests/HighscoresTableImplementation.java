package ninja.javahacker.temp.pipatest.tests;

import java.util.function.Supplier;
import ninja.javahacker.temp.pipatest.HighscoresTable;

/**
 * Enum to hold the two {@link HighscoresTable} implementations.
 * @author Victor Williams Stafusa da Silva
 */
public enum HighscoresTableImplementation {
    CAS(HighscoresTable::getCasImplementation),
    SYNC(HighscoresTable::getSynchronizedImplementation);

    private final Supplier<HighscoresTable> factory;

    private HighscoresTableImplementation(Supplier<HighscoresTable> factory) {
        this.factory = factory;
    }

    public HighscoresTable createTable() {
        return factory.get();
    }
}
