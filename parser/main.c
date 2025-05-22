#include <stdio.h>

#include "gemtext.tab.h"

void insert_token(const char *type, const char *value) {
	printf("%s: %s\n", type, value);
}

void yyerror(const char *s) {
	fprintf(stderr, "Parse error: %s\n", s);
}

int main() {
	yyparse();
	return 0;
}
