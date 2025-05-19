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
	char *newline = strchr(clean, '\n');
	if (newline) {
		*newline = '\0';
	}

	return clean;
}

int return_verbatim(const char *yytext) {
	yylval.str = strdup(yytext);
	return _VERBATIM;
}
%}

%%

"```"[^\n]*\n? {
	is_preformatted_mode = !is_preformatted_mode;
	yylval.str = strdup(yytext + 3);
	return _VERBATIM;
}

"=>"[ \t]*([^ \t\n]+)([ \t]+.*)?\n? {
	if (!is_preformatted_mode) {
		const char *start = yytext + 2;
		while (*start == ' ' || *start == '\t') {
			start++;
		}
		yylval.str = strdup(start);
		return LINK;
	}
	return return_verbatim(yytext);
}

###([ \t]+)([^\n]*)\n? {
	if (!is_preformatted_mode) {
		yylval.str = extract_trimmed_content(yytext, 3);
		return H3;
	}
	return return_verbatim(yytext);
}
##([ \t]+)([^\n]*)\n? {
	if (!is_preformatted_mode) {
		yylval.str = extract_trimmed_content(yytext, 2);
		return H2;
	}
	return return_verbatim(yytext);
}
#([ \t]+)([^\n]*)\n? {
	if (!is_preformatted_mode) {
		yylval.str = extract_trimmed_content(yytext, 1);
		return H1;
	}
	return return_verbatim(yytext);
}

"\*"[ \t]+([^\n]*)\n? {
	if (!is_preformatted_mode) {
		yylval.str = extract_trimmed_content(yytext, 2);
		return LIST;
	}
	return return_verbatim(yytext);
}

">"[ \t]+([^\n]*)\n? {
	if (!is_preformatted_mode) {
		yylval.str = extract_trimmed_content(yytext, 2);
		return QUOTE;
	}
	return return_verbatim(yytext);
}

[ \t]+  {}
\n {
	if (!is_preformatted_mode) {
		return NEWLINE;
	}
	return return_verbatim(yytext);
}

[^ \t\n][^\n]*\n? {
	yylval.str = strdup(yytext);
	return !is_preformatted_mode ? TEXT : _VERBATIM;
}

%%

int yywrap(void) {
	return 1;
}
