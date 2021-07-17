package java.lang;

// Shim for compilation under Java 6
public interface AutoCloseable {

    void close() throws Exception;
}
