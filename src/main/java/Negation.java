/*
 * The MIT License
 * Copyright © 2021 Dylan Middendorf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.TreeMap;

public class Negation {
    // File signature of "!"
    public static final int[] FILE_SIGNATURE = { '!', '-' };
    // End of file
    public static final int EOF = -0x1;
    // Whitespace
    public static final int TAB = 0x9;
    public static final int LF = 0xA;
    public static final int CR = 0xD;
    public static final int SPACE = 0x20;
    // Variable type declaration
    public static final int BOOLEAN = 0x21;
    public static final int NUMBER = 0x24;
    public static final int STRING = 0x5F;
    // Standard out
    public static final int STDOUT = 0x23;
    // Assignment operator
    public static final int ASSIGNMENT_OPERATOR = 0x3F;
    // Special characters
    public static final int SLASH = 0x2F;
    public static final int REVERSE_SOLIDUS = 0x5C;
    // Response codes
    public static final int SUCCESS = 0;

    // The input "!" program
    private final Reader in;
    // Counts the number of LF's and CR's found in the file. Used for debugging.
    private int lineNumber = 0;

    private boolean throwOnEOF = false;

    // Used for name-value pairs
    private final TreeMap<String, Boolean> booleanVariables = new TreeMap<>();
    private final TreeMap<String, Number> numberVariables = new TreeMap<>();
    private final TreeMap<String, String> stringVariables = new TreeMap<>();

    public Negation(File in) throws FileNotFoundException {
        NegationUtils.requireNonNull(in);
        this.in = new FileReader(in);
    }

    public Negation(InputStream in) {
        NegationUtils.requireNonNull(in);
        this.in = new InputStreamReader(in);
    }

    public Negation(Reader in) {
        NegationUtils.requireNonNull(in);
        this.in = in;
    }

    public Negation(String in) {
        NegationUtils.requireNonNull(in);
        this.in = new StringReader(in);
    }

    public void runNegation() throws IOException {
        NegationUtils.checkFileSignature(in);

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
                if (stringVariables.containsKey(message)) {
                    System.out.print(stringVariables.get(message));
                } else
                    System.out.print(message);
                break;
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
        int char0 = 0;
        int char1 = 0;
        while (true)
            switch (char0 = in.read()) {
            case EOF:
            case LF:
            case CR:
            case ASSIGNMENT_OPERATOR:
                return name.toString();
            case TAB:
            case SPACE:
                StringBuilder whitespace = new StringBuilder(Character.toString(char0));
                while ((char1 = in.read()) == TAB || char1 == SPACE)
                    whitespace.append((char) char1);
                switch (char1) {
                case EOF:
                case CR:
                case LF:
                    name.append(whitespace.toString());
                case ASSIGNMENT_OPERATOR:
                    NegationUtils.delete(whitespace);
                    return name.toString();
                default:
                    name.append(whitespace.toString() + (char) char1);
                    NegationUtils.delete(whitespace);
                }
                break;
            default:
                name.append((char) char0);
            }

    }

    private boolean readBoolean() throws IOException {
        String name = readName();
        boolean value = true;
        in.read();
        for (int c; (c = in.read()) != CR && c != LF;) {
            switch (c) {
            case EOF:
                throw new EOFException();
            case BOOLEAN:
                value = !value;
                break;
            default:
                throw new FileFormatException();
            }
        }
        booleanVariables.put(name, value);
        return value;
    }

    private int readNumber() throws IOException {
        String name = readName();
        StringBuilder value = new StringBuilder();
        for (int index = 0, n;; ++index) // index might be used in future releases
            switch (n = in.read()) {
            case EOF:
                throw new EOFException("Unexpected EOF while parsing number");
            case CR:
            case LF:
                int result = Integer.parseInt(value.toString());
                numberVariables.put(name, result);
                return result;
            case SPACE:
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
        in.read();
        StringBuilder value = new StringBuilder();
        for (int index = 0, n;; ++index) // index might be used in future releases
            switch (n = in.read()) {
            case EOF:
                throw new EOFException("Unexpected EOF while parsing string");
            case CR:
            case LF:
                String result = value.toString().replace("\\n", "\n");

                stringVariables.put(name, result);
                return result;
            case SLASH:
                value.append((char) REVERSE_SOLIDUS);
                break;
            default:
                value.append((char) n);
            }
    }

    final private static class NegationUtils {
        public static <T> T requireNonNull(T obj) {
            if (obj == null) {
                throw new NullPointerException("in == null");
            }
            return obj;
        }

        public static void checkFileSignature(Reader in) throws IOException {
            for (int expectedChar : FILE_SIGNATURE) {
                if (expectedChar != in.read()) {
                    throw new FileFormatException();
                }
            }
        }

        public static void delete(Object obj) {
            obj = null;
        }
    }

}
