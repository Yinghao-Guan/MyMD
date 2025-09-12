package com.guaguaaaa.mymd;

import com.guaguaaaa.mymd.pandoc.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for {@link PandocAstVisitor}.
 */
public class PandocAstVisitorTest {

    private PandocAstVisitor visitor;

    @BeforeEach
    void setUp() {
        // Create a fresh visitor instance before each test method is executed
        visitor = new PandocAstVisitor();
    }

    /**
     * Helper method: Parses an input MyMD string into a Pandoc AST.
     *
     * @param inputMyMD Input text in MyMD format
     * @return A PandocAst object produced from parsing
     */
    private PandocAst parseToPandocAst(String inputMyMD) {
        CharStream input = CharStreams.fromString(inputMyMD);
        MyMDLexer lexer = new MyMDLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MyMDParser parser = new MyMDParser(tokens);
        ParseTree tree = parser.document();
        return (PandocAst) visitor.visit(tree);
    }

    @Test
    @DisplayName("Test: a simple paragraph should be parsed correctly")
    void testSimpleParagraphWithSpace() {
        // 1. Prepare the input
        String mymdInput = "Hello world";

        // 2. Execute the method under test
        PandocAst resultAst = parseToPandocAst(mymdInput);

        // 3. Assertions (verifying the result)
        assertNotNull(resultAst, "AST should not be null");

        // Verify that the blocks list is not empty and contains exactly one element
        List<Block> blocks = resultAst.getBlocks();
        assertNotNull(blocks, "Blocks list should not be null");
        assertEquals(1, blocks.size(), "There should be exactly one Block");

        // Verify that the first block is of type Para
        Object firstBlock = blocks.get(0);
        assertTrue(firstBlock instanceof Para, "The first Block should be of type Para");

        // Verify the contents of the Para
        Para para = (Para) firstBlock;
        Object paraContent = para.c;
        assertNotNull(paraContent, "The contents of the Para should not be null");
        assertTrue(paraContent instanceof List, "The contents of the Para should be a List");

        List<?> inlines = (List<?>) paraContent;
        assertEquals(3, inlines.size(), "The Para content list should contain three elements: Str, Space, Str");

        // Verify that the first element is Str("Hello")
        assertTrue(inlines.get(0) instanceof Str, "The first inline element should be Str");
        assertEquals("Hello", ((Str) inlines.get(0)).c);

        // Verify that the second element is Space
        assertTrue(inlines.get(1) instanceof Space, "The second inline element should be Space");

        // Verify that the third element is Str("world")
        assertTrue(inlines.get(2) instanceof Str, "The third inline element should be Str");
        assertEquals("world", ((Str) inlines.get(2)).c);
    }

    @Test
    @DisplayName("Test: H1 header should be parsed correctly")
    void testH1Header() {
        // 1. Prepare the input
        String mymdInput = "# My Title";

        // 2. Execute the method under test
        PandocAst resultAst = parseToPandocAst(mymdInput);

        // 3. Assertions (verifying the result)
        assertNotNull(resultAst, "AST should not be null");

        // Verify the blocks list
        List<Block> blocks = resultAst.getBlocks();
        assertNotNull(blocks, "Blocks list should not be null");
        assertEquals(1, blocks.size(), "There should be exactly one Block");

        // Verify that the first block is of type Header
        Object firstBlock = blocks.get(0);
        assertTrue(firstBlock instanceof Header, "The first Block should be of type Header");

        // Verify the contents of the Header
        Header header = (Header) firstBlock;
        Object headerContent = header.c;
        assertNotNull(headerContent, "The contents of the Header should not be null");
        assertTrue(headerContent instanceof List, "The contents of the Header should be a List");

        List<?> headerData = (List<?>) headerContent;
        assertEquals(3, headerData.size(), "The Header data list should contain three parts: level, attributes, inlines");

        // Verify the header level
        Object level = headerData.get(0);
        assertTrue(level instanceof Integer, "The header level should be of type Integer");
        assertEquals(1, (Integer) level, "The header level should be 1");

        // Verify the inline elements of the header
        Object inlinesObject = headerData.get(2);
        assertTrue(inlinesObject instanceof List, "The inline elements should be a List");
        List<?> inlines = (List<?>) inlinesObject;
        assertEquals(3, inlines.size(), "The inline elements list should contain three items: Str, Space, Str");

        // Verify the specific inline elements
        assertTrue(inlines.get(0) instanceof Str && "My".equals(((Str) inlines.get(0)).c));
        assertTrue(inlines.get(1) instanceof Space);
        assertTrue(inlines.get(2) instanceof Str && "Title".equals(((Str) inlines.get(2)).c));
    }

}
