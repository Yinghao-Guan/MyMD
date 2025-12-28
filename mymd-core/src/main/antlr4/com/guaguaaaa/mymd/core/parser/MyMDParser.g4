parser grammar MyMDParser;

options { tokenVocab=MyMDLexer; }

// ======================= Parser Rules =======================

doc
    : yaml_block? (block | PARAGRAPH_END | SOFT_BREAK | SPACE)* EOF
    ;

yaml_block
    : YAML_BLOCK
    ;

block
    : horizontalRule          # HorizontalRuleBlock
    | blockquote              # BlockQuoteBlock
    | bulletListBlock         # BulletListRule
    | codeBlock               # CodeBlockRule
    | header                  # HeaderRule
    | blockMath               # BlockMathRule
    | paragraph               # ParagraphBlock
    ;

horizontalRule
    : (DASH DASH DASH+ | STAR STAR STAR+) (SOFT_BREAK | PARAGRAPH_END | EOF)
    ;

blockquote
    : GT SPACE? inline+ (PARAGRAPH_END | EOF)
    ;

header
    : (H1 | H2 | H3 | H4 | H5 | H6) inline+ (PARAGRAPH_END | EOF)
    ;

paragraph
    : inline+ (
          PARAGRAPH_END
        | EOF
        | { _input.LA(1) == BLOCK_MATH
        || _input.LA(1) == CODE_BLOCK
        || _input.LA(1) == H1
        || _input.LA(1) == H2
        || _input.LA(1) == H3
        || _input.LA(1) == H4
        || _input.LA(1) == H5
        || _input.LA(1) == H6}?
      )
    ;

blockMath
    : BLOCK_MATH (PARAGRAPH_END | EOF)?
    ;

bulletListBlock
    : bulletList (PARAGRAPH_END | EOF)?
    ;

codeBlock
    : CODE_BLOCK (PARAGRAPH_END | EOF)?
    ;

bulletList
    : listItem+
    ;

listItem
    : DASH SPACE inline+ (SOFT_BREAK | PARAGRAPH_END)
    ;

inline
    : image                   # ImageInline
    | link                    # LinkInline
    | bold                    # BoldInline
    | italic                  # ItalicInline
    | INLINE_MATH             # InlineMathInline
    | INLINE_CODE             # InlineCodeInline
    | citation                # CitationInline
    | lbracket                # LBracketInline
    | rbracket                # RBracketInline
    | bang                    # BangInline
    | gt                      # GtInline
    | lparen                  # LParenInline
    | rparen                  # RParenInline
    | urlText                 # UrlTextInline
    | dash                    # DashInline
    | star                    # StarInline
    | HARD_BREAK              # HardBreakInline
    | SOFT_BREAK              # SoftBreakInline
    | ESCAPED                 # EscapedInline
    | TEXT                    # TextInline
    | SPACE                   # SpaceInline
    ;

citation : CITATION ;
lbracket : LBRACKET ;
rbracket : RBRACKET ;

// 使用 BOLD_MARK
bold     : BOLD_MARK inline+ BOLD_MARK ;
italic   : STAR inline+ STAR ;

bang     : BANG ;
gt       : GT ;
lparen   : LPAREN ;
rparen   : RPAREN ;
urlText  : URL_TEXT ;
dash     : DASH ;
star     : STAR ;

image : BANG LBRACKET inline* RBRACKET LPAREN url RPAREN ;
link  : LBRACKET inline+ RBRACKET LPAREN url RPAREN ;

url : (URL_TEXT | DASH | STAR | TEXT)+ ;