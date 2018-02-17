package tee;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class TeeTests {
    private static int passedTests = 0;
    private static int failedTests = 0;

    private static final String FILENAME = "text.txt";
    private static final Path PATH = Paths.get(FILENAME);
    private static final byte[] DATA_EMPTY = new byte[0];
    private static final byte[] DATA_NOT_EMPTY =
            "tee - Copies standard input to standard output, and also to files".getBytes(StandardCharsets.UTF_8);

    public static void main(String[] args) throws IOException {
        test(1, DATA_EMPTY, false, DATA_EMPTY, DATA_EMPTY);
        test(2, DATA_NOT_EMPTY, false, DATA_NOT_EMPTY, DATA_NOT_EMPTY);
        test(3, DATA_EMPTY, true, DATA_NOT_EMPTY, DATA_NOT_EMPTY);
        test(4, DATA_NOT_EMPTY, true, DATA_NOT_EMPTY, merge(DATA_NOT_EMPTY, DATA_NOT_EMPTY));
        System.err.println("Tests run: " + (passedTests + failedTests) + ". Failures: " + failedTests + '.');
    }

    private static void test(int testNum, byte[] data, boolean append, byte[] fileContent, byte[] expectedFile) throws IOException {
        System.setIn(new ByteArrayInputStream(data));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        Files.write(PATH, fileContent);
        if (append) {
            Tee.main("-a", FILENAME);
        } else {
            Tee.main(FILENAME);
        }

        if (!Arrays.equals(out.toByteArray(), data)) {
            failedTests++;
            System.err.println("Test " + testNum + " failed\n");
            return;
        }
        if (!Arrays.equals(Files.readAllBytes(PATH), expectedFile)) {
            failedTests++;
            System.err.println("Test " + testNum + " failed\n");
            return;
        }
        passedTests++;
    }

    private static byte[] merge(byte[] first, byte[] second) {
        byte[] newArray = new byte[first.length + second.length];
        System.arraycopy(first, 0, newArray, 0, first.length);
        System.arraycopy(second, 0, newArray, first.length, second.length);
        return newArray;
    }

}
