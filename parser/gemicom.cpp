#include <string.h>
#include <stdlib.h>

#include <jni.h>

extern "C" {
	#include "gemtext.tab.h"
}

#define LOG_TAG "Capsule"

typedef struct {
	const char *type;
	char *value;
} Token;

static Token *tokens = NULL;
static int token_count = 0;
static int token_capacity = 0;
static int gemtext_parse_error = 0;

extern "C" {
	typedef void *YY_BUFFER_STATE;
	YY_BUFFER_STATE yy_scan_string(const char *yy_str);
	void yy_delete_buffer(YY_BUFFER_STATE b);
	void yy_switch_to_buffer(YY_BUFFER_STATE new_buffer);
	int yyparse(void);
	void yyerror(const char *s);
}

static int grow_tokens() {
	if (token_count >= token_capacity) {
		int new_capacity = token_capacity == 0 ? 32 : token_capacity * 2;
		
		Token *new_tokens = (Token *)realloc(tokens, sizeof(Token) * new_capacity);
		if (!new_tokens) {
			return 0;
		}

		tokens = new_tokens;
		token_capacity = new_capacity;
	}

	return 1;
}

extern "C" void insert_token(const char *type, const char *value) {
	if (!grow_tokens()) {
		return;
	}

	tokens[token_count].type = type;
	tokens[token_count].value = strdup(value);
	token_count++;
}

extern "C" void yyerror(const char *s) {
	gemtext_parse_error = 1;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_app_gemicom_lib_Gemini_parse(
	JNIEnv *env,
	jobject /* this */,
	jstring content
) {
	const char *input = env->GetStringUTFChars(content, nullptr);
	if (!input) {
		return nullptr;
	}

	token_count = 0;
	gemtext_parse_error = 0;

	YY_BUFFER_STATE buffer = yy_scan_string(input);
	yy_switch_to_buffer(buffer);
	yyparse();
	yy_delete_buffer(buffer);

	env->ReleaseStringUTFChars(content, input);

	jclass tokenCls = env->FindClass("app/gemicom/lib/Token");
	jmethodID constructor = env->GetMethodID(
		tokenCls, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"
	);

	jobjectArray result = env->NewObjectArray(token_count, tokenCls, nullptr);

	for (int i = 0; i < token_count; ++i) {
		jstring typeStr = env->NewStringUTF(tokens[i].type);
		jstring valueStr = env->NewStringUTF(tokens[i].value);
		jobject token = env->NewObject(tokenCls, constructor, typeStr, valueStr);

		env->SetObjectArrayElement(result, i, token);
		free(tokens[i].value);
	}

	free(tokens);
	tokens = nullptr;
	token_capacity = 0;
	token_count = 0;

	return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_app_gemicom_lib_Gemini_lasterror(JNIEnv *, jclass) {
	return gemtext_parse_error ? JNI_TRUE : JNI_FALSE;
}
