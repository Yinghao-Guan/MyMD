parser grammar MyMDParser;

options { tokenVocab=MyMDLexer; }

// ======================= Parser Rules =======================

doc
    : yaml_block?
    (block | PARAGRAPH_END | SOFT_BREAK | SPACE)* EOF
    ;

yaml_block
    : YAML_BLOCK
    ;

block
    : horizontalRule          # HorizontalRuleBlock
    | blockquote              # BlockQuoteBlock
    | { _input.LA(1) == DASH && _input.LA(2) == SPACE }? bulletListBlock   # BulletListRule
    | codeBlock               # CodeBlockRule
    | header                  # HeaderRule
    | blockMath               # BlockMathRule
    | paragraph               # ParagraphBlock
    ;

horizontalRule
    : HR (SOFT_BREAK | PARAGRAPH_END | EOF)?
    ;

blockquote
    : GT SPACE? inline+ (PARAGRAPH_END | EOF)
    ;

header
    : (H1 | H2 | H3 | H4 | H5 | H6) inline+ (PARAGRAPH_END | EOF)
    ;

paragraph
    : inlineNoBreak+
      (
        { _input.LA(1) == SOFT_BREAK && !(_input.LA(2) == DASH && _input.LA(3) == SPACE) }?
        SOFT_BREAK
        inlineNoBreak+
      )*
      (
          PARAGRAPH_END
        | EOF
        | {
            _input.LA(1) == BLOCK_MATH ||
            _input.LA(1) == CODE_BLOCK ||
            _input.LA(1) == H1 || _input.LA(1) == H2 || _input.LA(1) == H3 ||
            _input.LA(1) == H4 || _input.LA(1) == H5 || _input.LA(1) == H6 ||
            (_input.LA(1) == DASH && _input.LA(2) == SPACE) ||
            // 特别处理：如果后面是软换行接列表（紧凑列表），也允许段落在此结束
            (_input.LA(1) == SOFT_BREAK && _input.LA(2) == DASH && _input.LA(3) == SPACE)
          }?
      )
    ;

blockMath
    : BLOCK_MATH SPACE? REF_ID? (PARAGRAPH_END | EOF)?
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
    : DASH SPACE
      inlineCommon+
      (
        { _input.LA(1) == HARD_BREAK && !(_input.LA(2) == DASH && _input.LA(3) == SPACE) }?
        HARD_BREAK
        inlineCommon+
      )*
      (
        { _input.LA(1) == HARD_BREAK }? HARD_BREAK
      )?
      (SOFT_BREAK | PARAGRAPH_END | EOF)?
    ;

inlineNoBreak
    : inline
    ;

inline
    : inlineCommon    # CommonInlineWrapper
    | HARD_BREAK      # HardBreakInline
    ;

inlineCommon
    : image                   # ImageInline
    | link                    # LinkInline
    | ref                     # RefInline
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
    | ESCAPED                 # EscapedInline
    | TEXT                    # TextInline
    | SPACE                   # SpaceInline
    ;


citation : CITATION ;
lbracket : LBRACKET ;
rbracket : RBRACKET ;

bold     : BOLD_MARK inline+ BOLD_MARK ;
italic   : STAR inline+ STAR ;

bang     : BANG ;
gt       : GT ;
lparen   : LPAREN ;
rparen   : RPAREN ;
urlText  : URL_TEXT ;
dash     : DASH ;
star     : STAR ;

link
    : (LBRACKET inline+ RBRACKET | REF_ID) LPAREN url RPAREN
    ;
image
    : BANG (LBRACKET inline* RBRACKET | REF_ID) LPAREN url RPAREN
    ;
url : (URL_TEXT | DASH | STAR | TEXT)+ ;

ref : REF_ID ;