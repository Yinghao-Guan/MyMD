package com.guaguaaaa.mymd.core.parser;

import com.guaguaaaa.mymd.core.api.Diagnostic;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

public class SyntaxErrorCollector extends BaseErrorListener {
    private final List<Diagnostic> errors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg, RecognitionException e) {
        errors.add(new Diagnostic(line, charPositionInLine, msg));
    }

    public List<Diagnostic> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}