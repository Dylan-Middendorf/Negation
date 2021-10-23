import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;
import java.util.TreeMap;

public class Negation {

    public static final int[] FILE_SIGNATURE = { '!', '-' };
    public static final int EOF = -1;
    public static final int NL = '\n';
    public static final int CR = 13;
    public static final int SP = ' ';
    public static final int BOOLEAN = '!';
    public static final int NUMBER = '$';
    public static final int STRING = '_';
    public static final int STDOUT = '#';
    public static final int ASSIGNMENT_OPERATOR = '?';
    // Response codes
    public static final int SUCCESS = 0;

    private Reader in;
    private int lineNumber = 0;

    private final TreeMap<String, Boolean> booleanVariables = new TreeMap<>();
    private final TreeMap<String, Integer> numberVariables = new TreeMap<>();
    private final TreeMap<String, String> stringVariables = new TreeMap<>();

    public Negation(Reader in) {
        Objects.requireNonNull(in);
        this.in = in;
    }

    public void runNegation() throws IOException {
        if (checkFileSignature() != SUCCESS)
            return;

        for (int response; (response = readLine()) != 200; ++lineNumber) {
            switch (response) {

            }
        }
    }

    public int readLine() throws IOException {
        int temp;
        switch (this.in.read()) {
        case BOOLEAN:
            readBoolean();
            break;
        case NUMBER:
            readNumber();
            break;
        case STRING:
            readString();
            break;
        case STDOUT:
            switch (temp = in.read()) {
            case BOOLEAN:
                System.out.print(booleanVariables.get(readName()) ? 1 : 0);
                break;
            case NUMBER:
                System.out.print(numberVariables.get(readName()));
                break;
            case STRING:
                String message = readName();
                if (message.equals("/n"))
                    System.out.println();
                else if (stringVariables.containsKey(message))
                    System.out.print(stringVariables.get(readName()));
                else
                    System.out.print(message);
                break;
            // default:
            // StringBuilder message = new StringBuilder((char) temp);
            // while (temp != -1)
            // switch (temp = in.read()) {
            // case NL:
            // temp = -1;
            // System.out.println(message);
            // break;
            // default:
            // message.append((char) temp);
            // }
            }
            break;
        case '-':
            if (in.read() == '!')
                return 200;
        case EOF:
            throw new EOFException("Expected closing file signature.");
        }
        return 0;
    }

    private String readName() throws IOException {
        StringBuilder name = new StringBuilder();
        for (int n;;)
            switch (n = in.read()) {
            case EOF:
                throw new EOFException("Unexpected EOF while parsing variable name at " + lineNumber);
            case NL:
            case CR:
            case ASSIGNMENT_OPERATOR:
                return name.toString();
            default:
                name.append((char) n);
            }
    }

    private boolean readBoolean() throws IOException {
        String name = readName();
        name = name.substring(0, name.length() - 1);
        boolean value = true;
        boolean isInitialized = false;
        for (int index = 0;; ++index) // index might be used in future releases
            switch (in.read()) {
            case EOF:
                throw new EOFException("Unexpected EOF while parsing boolean");
            case CR:
            case NL:
                booleanVariables.put(name, value);
                return value;
            case BOOLEAN:
                if (isInitialized && value)
                    throw new FileFormatException(
                            "The boolean formmating does not follow the file format at line " + lineNumber);
                isInitialized = true;
                value = !value;
            }
    }

    private int readNumber() throws IOException {
        String name = readName();
        StringBuilder value = new StringBuilder();
        for (int index = 0, n;; ++index) // index might be used in future releases
            switch (n = in.read()) {
            case EOF:
                throw new EOFException("Unexpected EOF while parsing number");
            case CR:
            case NL:
                int result = Integer.parseInt(value.toString());
                numberVariables.put(name, result);
                return result;
            case SP:
                break;
            default:
                if (n < 48 || (57 < n && n < 97) || 123 < n)
                    throw new FileFormatException(
                            "Unable to parse character '" + ((char) n) + "' at line " + lineNumber);
                value.append((char) n);
            }
    }

    private String readString() throws IOException {
        String name = readName();
        StringBuilder value = new StringBuilder();
        for (int index = 0, n;; ++index) // index might be used in future releases
            switch (n = in.read()) {
            case EOF:
                throw new EOFException("Unexpected EOF while parsing string");
            case CR:
            case NL:
                String result = value.toString();
                stringVariables.put(name, result);
                return result;
            case SP:
                break;
            default:
                value.append((char) n);
            }
    }

    public int checkFileSignature() throws IOException {
        for (int expectedChar : FILE_SIGNATURE) {
            if (expectedChar != this.in.read())
                return 1;
        }
        return 0;
    }

    final static class FileFormatException extends IOException {
        private static final long serialVersionUID = 1L;

        public FileFormatException() {
            super();
        }

        public FileFormatException(String s) {
            super(s);
        }

        public FileFormatException(int lineNumber) {
            super("Illegal syntax at line " + lineNumber);
        }
    }
}
