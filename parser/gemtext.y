/* Gemtext BNF

   gemtext-document = 1*gemtext-line
   gemtext-line     = text-line / link-line / preformat-toggle / heading / list-line / quote-line
   link-line        = "=>" SP URI-reference [SP 1*(SP / VCHAR)] CRLF
   heading          = ( "#" / "##" / "###" ) text-line
   list-line        = "*" SP text-line
   quote-line       = ">" SP text-line
   preformat-toggle = "```" text-line
   text-line        = *(SP / VCHAR) CRLF

   End of Gemtext BNF */

%{
#include <stdio.h>
#include <stdlib.h>

void yyerror(const char *s);
int yylex(void);

extern void insert_token(const char *type, const char *value);
%}

%union {
	char *str;
}

%token <str> LINK HEADING LIST QUOTE TEXT _VERBATIM
%token PREFORMAT NEWLINE

%%

document:
    lines
    ;

lines:
    line
    | lines line
    ;

line:
    LINK        { insert_token("LINK", $1); free($1); }
    | HEADING   { insert_token("HEADING", $1); free($1); }
    | LIST      { insert_token("LIST", $1); free($1); }
    | QUOTE     { insert_token("QUOTE", $1); free($1); }
    | _VERBATIM { insert_token("PREFORMAT", $1); free($1); }
    | TEXT      { insert_token("TEXT", $1); free($1); }
    | NEWLINE   { insert_token("NEWLINE", ""); }
    | PREFORMAT {}
    ;
%%
