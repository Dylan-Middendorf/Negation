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
import java.util.LinkedList;
import java.util.TreeMap;

public class Negation {
    // File signature of "!"
    public static final int[] FILE_SIGNATURE = { '!', '-' };
    // End of file
    public static final int EOF = -0x1;
    // Whitespace
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
    private int currentChar = 0;

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
        switch (this.in.read()) {
        case BOOLEAN:
            readBoolean();
            break;
        case NUMBER:
            readNumber();
            break;
        case STRING:
            LinkedList<String> chainedAssignments = new LinkedList<>();
            // Fill the list
            do {
                chainedAssignments.add(readString());
            } while (currentChar == ASSIGNMENT_OPERATOR);
            // Empty the list
            while (chainedAssignments.size() > 1)
                stringVariables.put(chainedAssignments.pollFirst(), chainedAssignments.getLast());
            break;
        case STDOUT:
            switch (in.read()) {
            case BOOLEAN:
                System.out.print(booleanVariables.get(readString()) ? 1 : 0);
                break;
            case NUMBER:
                System.out.print(numberVariables.get(readString()));
                break;
            case STRING:
                String message = readString();
                if (stringVariables.containsKey(message))
                    message = stringVariables.get(message);

                message = message.replace("/n", "\n");
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

    private String readString() throws IOException {
        StringBuilder str;
        if (currentChar == ASSIGNMENT_OPERATOR)
            str = new StringBuilder(Character.toString(nextNonWhitespace()));
        else
            str = new StringBuilder();
        int char0 = 0;
        int char1 = 0;
        while (true)
            switch (char0 = in.read()) {
            case EOF:
            case LF:
            case CR:
            case ASSIGNMENT_OPERATOR:
                this.currentChar = char0;
                return str.toString();
            case SPACE:
                StringBuilder whitespace = new StringBuilder(Character.toString(char0));
                while ((char1 = in.read()) == SPACE)
                    whitespace.append((char) char1);
                switch (char1) {
                case EOF:
                case CR:
                case LF:
                    str.append(whitespace);
                case ASSIGNMENT_OPERATOR:
                    this.currentChar = char1;
                    return str.toString();
                default:
                    str.append(whitespace.toString() + (char) char1);
                }
                break;
            default:
                str.append((char) char0);
            }

    }

    private boolean readBoolean() throws IOException {
        String name = readString();
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
        String name = readString();
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

    private int nextNonWhitespace() throws IOException {
        int char0;
        while (true) {
            char0 = in.read();
            if (char0 == EOF)
                break;
            if (char0 == SPACE)
                continue;
            return char0;
        }
        if (throwOnEOF)
            throw new EOFException("End of input");
        else
            return -1;
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
    }

}
