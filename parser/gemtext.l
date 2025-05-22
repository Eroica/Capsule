%{
#include <string.h>
#include <stdlib.h>

#include "gemtext.tab.h"

int is_preformatted_mode;
%}

%%

"```"[^\n]*\n? {
	is_preformatted_mode = !is_preformatted_mode;
	yylval.str = strdup(yytext + 3);
	return _VERBATIM;
}
"=>"[ \t]*([^ \t\n]+)([ \t]+.*)?\n? {
	if (!is_preformatted_mode) {
		const char* start = yytext + 2;
		while (*start == ' ' || *start == '\t') {
			start++;
		}
		yylval.str = strdup(start);
		return LINK;
	} else {
		yylval.str = strdup(yytext);
		return _VERBATIM;
	}
}
#{1,3}[ \t]+([^\n]*)\n? {
	if (!is_preformatted_mode) {
		yylval.str = strdup(yytext + strcspn(yytext, " \t") + 1);
		return HEADING;
	} else {
		yylval.str = strdup(yytext);
		return _VERBATIM;
	}
}
"\*"[ \t]+([^\n]*)\n? {
	if (!is_preformatted_mode) {
		yylval.str = strdup(yytext + 2);
		return LIST;
	} else {
		yylval.str = strdup(yytext);
		return _VERBATIM;
	}
}
">"[ \t]+([^\n]*)\n? {
	if (!is_preformatted_mode) {
		yylval.str = strdup(yytext + 2);
		return QUOTE;
	} else {
		yylval.str = strdup(yytext);
		return _VERBATIM;
	}
}
[ \t]+  {}
\n {
	if (!is_preformatted_mode) {
		return NEWLINE;
	} else {
		yylval.str = strdup(yytext);
		return _VERBATIM;
	}
}
[^ \t\n][^\n]*\n? {
	yylval.str = strdup(yytext);
	return !is_preformatted_mode ? TEXT : _VERBATIM;
}

%%

int yywrap(void) {
	return 1;
}
