package tee;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * tee - Copies standard input to standard output, and also to files
 */
public final class Tee {
    private static final String VERSION = "1.0";
    private static final String HELP =
            "Usage: tee [OPTION]... [FILE]...\n" +
            "Copy standard input to each FILE, and also to standard output.\n\n" +
            "  -a, --append              append to the given FILEs, do not overwrite\n" +
            "      --help     display this help and exit\n" +
            "      --version  output version information and exit";
    private static final int BUFFER_SIZE = 4096;

    private Tee() {
    }
    
    public static void main(String... args) {
        Options options = parse(args);

        if (options == Options.HELP) {
            System.out.println(HELP);
            return;
        }

        if (options == Options.VERSION) {
            System.out.println("tee v" + VERSION);
            return;
        }

        if (options instanceof WrongOption) {
            System.err.println("Invalid option: '" + ((WrongOption) options).arg + "'");
            System.err.println("Try 'tee --help' for more information.");
            System.exit(1);
            return;
        }

        if (options instanceof WriteOptions) {
            List<Output> outputs = getOutputs((WriteOptions) options);
            try {
                writeToOutputs(outputs);
            } finally {
                // Close all files
                for (Output output : outputs) {
                    try {
                        output.outputStream.close();
                    } catch (IOException e) {
                        System.err.println("Exception occurred while closing the file '" + output.fileName + "': " + e);
                    }
                }
            }
            return;
        }
    }

    /**
     * Opens files from options and wraps them in outputs
     */
    private static List<Output> getOutputs(WriteOptions options) {
        List<Output> outputs = new LinkedList<>();
        outputs.add(new Output(System.out, "stdout"));
        for (Path path : options.files) {
            try {
                OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                        options.append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING);
                outputs.add(new Output(new BufferedOutputStream(outputStream), path.toString()));
            } catch (IOException e) {
                System.err.println("Exception occurred while opening file '" + path + "': " + e);
            }
        }
        return outputs;
    }

    /**
     * Write the standard input into each file and into the standard output.
     */
    private static void writeToOutputs(List<Output> outputs) {
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            int length = System.in.read(buffer);
            while (length != -1) {
                Iterator<Output> iterator = outputs.iterator();
                while (iterator.hasNext()) {
                    Output output = iterator.next();
                    try {
                        output.outputStream.write(buffer, 0, length);
                    } catch (IOException e) {
                        try {
                            output.outputStream.close();
                        } catch (IOException e1) {
                            e.addSuppressed(e1);
                        }
                        iterator.remove();
                        System.err.println("Exception occurred while writing to the file '" + output.fileName + "': " + e);
                    }
                }
                length = System.in.read(buffer);
            }
        } catch (IOException e) {
            System.err.println("Exception occurred while reading input: " + e);
        }
    }

    /**
     * Parses command line arguments and wraps them in Options object
     */
    private static Options parse(String... args) {
        boolean append = false;
        List<Path> files = new ArrayList<>(args.length);
        for (String arg : args) {
            if (arg.isEmpty()) {
                continue;
            }
            if (!arg.startsWith("-")) {
                files.add(Paths.get(arg));
                continue;
            }

            if (arg.startsWith("--")) {
                switch (arg) {
                    case "--help":
                        return Options.HELP;
                    case "--version":
                        return Options.VERSION;
                    case "--append":
                        append = true;
                        break;
                    default:
                        return new WrongOption(arg);
                }
            }
            if (arg.length() <= 1) {
                return new WrongOption(arg);
            }

            for (int i = 1; i < arg.length(); i++) {
                char ch = arg.charAt(i);
                switch (ch) {
                    case 'a':
                        append = true;
                        break;
                    default:
                        return new WrongOption("-" + ch);
                }
            }
        }
        return new WriteOptions(append, files);
    }

    /**
     * Base class for all kinds of options
     */
    private static class Options {
        private static final Options HELP = new Options();
        private static final Options VERSION = new Options();
    }

    private static final class WrongOption extends Options {
        private final String arg;

        private WrongOption(String arg) {
            this.arg = arg;
        }
    }

    private static final class WriteOptions extends Options {
        private final boolean append;
        private final List<Path> files;

        private WriteOptions(boolean append, List<Path> files) {
            this.append = append;
            this.files = files;
        }
    }

    /**
     * Associates output streams with their file names
     */
    private static final class Output {
        private final OutputStream outputStream;
        private final String fileName;

        private Output(OutputStream outputStream, String fileName) {
            this.outputStream = outputStream;
            this.fileName = fileName;
        }
    }
}
