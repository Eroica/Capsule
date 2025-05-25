%{
#include <string.h>
#include <stdlib.h>

#include "gemtext.tab.h"

int is_preformatted_mode;

char* extract_trimmed_content(const char *yytext, size_t prefix_len) {
	const char *start = yytext + prefix_len;
	while (*start == ' ' || *start == '\t') {
		start++;
	}

	char *clean = strdup(start);
	size_t len = strlen(clean);
	while (len > 0 && (clean[len - 1] == '\n' || clean[len - 1] == '\r')) {
		clean[--len] = '\0';
	}

	return clean;
}

int return_verbatim(const char *yytext) {
	yylval.str = strdup(yytext);
	return _VERBATIM;
}

int return_token_or_verbatim(const char *yytext, int prefix_len, int token) {
	if (!is_preformatted_mode) {
		yylval.str = extract_trimmed_content(yytext, prefix_len);
		return token;
	}

	return return_verbatim(yytext);
}
%}

%%

"```"[^\r\n]*\r?\n {
	is_preformatted_mode = !is_preformatted_mode;
	yylval.str = strdup(yytext + 3);
	return _VERBATIM;
}

"=>"[ \t]*([^ \t\r\n]+)([ \t]+.*)?\r?\n { return return_token_or_verbatim(yytext, 2, LINK); }
#([ \t]+)([^\r\n]*)\r?\n { return return_token_or_verbatim(yytext, 1, H1); }
##([ \t]+)([^\r\n]*)\r?\n { return return_token_or_verbatim(yytext, 2, H2); }
###([ \t]+)([^\r\n]*)\r?\n { return return_token_or_verbatim(yytext, 3, H3); }
"\*"[ \t]+([^\r\n]*)\r?\n { return return_token_or_verbatim(yytext, 2, LIST); }
">"[ \t]+([^\r\n]*)\r?\n { return return_token_or_verbatim(yytext, 2, QUOTE); }

[ \t]+  {}
\r?\n {
	if (!is_preformatted_mode) {
		return NEWLINE;
	}
	return return_verbatim(yytext);
}

[^ \t\r\n][^\r\n]*\r?\n { return return_token_or_verbatim(yytext, 0, TEXT); }

%%

int yywrap(void) {
	return 1;
}
