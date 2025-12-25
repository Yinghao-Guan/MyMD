package com.guaguaaaa.mymd.core.api;

public class Diagnostic {
    public final int line;
    public final int column;
    public final String message;

    public Diagnostic(int line, int column, String message) {
        this.line = line;
        this.column = column;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Line " + line + ":" + column + " " + message;
    }
}