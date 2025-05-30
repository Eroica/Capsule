%{
#include <string.h>
#include <stdlib.h>

#include "gemtext.tab.h"

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

int trimmed_token(const char *yytext, int prefix_len, int token) {
	yylval.str = extract_trimmed_content(yytext, prefix_len);
	return token;
}
%}

EOL (\r\n|\r|\n)

%x PRE

%%

<PRE>"```"[^\r\n]*{EOL}? {
    BEGIN(INITIAL);
    /* return a token if you want (or ignore) */
    yylval.str = strdup(yytext + 3);
    return PREFORMAT;
}

<PRE>[^\r\n]*{EOL}? {
    yylval.str = strdup(yytext);
    return PREFORMAT;
}

"```"[^\r\n]*{EOL}? {
    BEGIN(PRE);
    yylval.str = strdup(yytext + 3);
    return PREFORMAT;
}

"=>"[ \t]*([^ \t\r\n]+)([ \t]+.*)?{EOL}? { return trimmed_token(yytext, 2, LINK); }
#([ \t]+)([^\r\n]*){EOL}? { return trimmed_token(yytext, 1, H1); }
##([ \t]+)([^\r\n]*){EOL}? { return trimmed_token(yytext, 2, H2); }
###([ \t]+)([^\r\n]*){EOL}? { return trimmed_token(yytext, 3, H3); }
"\*"[ \t]+([^\r\n]*){EOL}? { return trimmed_token(yytext, 2, LIST); }
">"[ \t]+([^\r\n]*){EOL}? { return trimmed_token(yytext, 2, QUOTE); }

[ \t]+  {}
{EOL}? {
	return NEWLINE;
}

[^ \t\r\n][^\r\n]*{EOL}? {
	return trimmed_token(yytext, 0, TEXT);
}

%%

int yywrap(void) {
	return 1;
}
