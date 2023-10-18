%{
#include <stdio.h>
#include "P1.tab.h"

int debugl = 1;

extern void yyerror(char *s);
%}

%%

[ \t\r\n] {if(debugl) printf("%s", yytext);}
"//"[^\n]* {if(debugl) printf("%s", yytext);}
\/\*(.|\n)*\*\/ {if(debugl) printf("%s", yytext);}

"#define" {if(debugl) printf("#define"); yylval.str = strdup("#define"); return HASH_DEFINE;}

"class" {if(debugl) printf("class"); yylval.str = strdup("class"); return CLASS;}
"public" {if(debugl) printf("public"); yylval.str = strdup("public"); return PUBLIC;}
"static" {if(debugl) printf("static"); yylval.str = strdup("static"); return STATIC;}
"void" {if(debugl) printf("void"); yylval.str = strdup("void"); return VOID;}
"main" {if(debugl) printf("main"); yylval.str = strdup("main"); return MAIN;}

"String" {if(debugl) printf("String"); yylval.str = strdup("String"); return STRING_DECL;}

"System.out.println" {if(debugl) printf("System.out.println"); yylval.str = strdup("System.out.println"); return PRINTLN;}

"extends" {if(debugl) printf("extends"); yylval.str = strdup("extends"); return EXTENDS;}

"return" {if(debugl) printf("return"); yylval.str = strdup("return"); return RETURN;}

"int" {if(debugl) printf("int"); yylval.str = strdup("int"); return INT;}
"boolean" {if(debugl) printf("boolean"); yylval.str = strdup("boolean"); return BOOLEAN;}

"if" {if(debugl) printf("if"); yylval.str = strdup("if"); return IF;}
"else" {if(debugl) printf("else"); yylval.str = strdup("else"); return ELSE;}

"do" {if(debugl) printf("do"); yylval.str = strdup("do"); return DO;}
"while" {if(debugl) printf("while"); yylval.str = strdup("while"); return WHILE;}

"true" {if(debugl) printf("true"); yylval.str = strdup("true"); return TRUE_TOKEN;}
"false" {if(debugl) printf("false"); yylval.str = strdup("false"); return FALSE_TOKEN;}
"this" {if(debugl) printf("this"); yylval.str = strdup("this"); return THIS;}
"new" {if(debugl) printf("new"); yylval.str = strdup("new"); return NEW;}

"[" {if(debugl) printf("["); yylval.str = strdup("["); return L_SQ_BR;}
"]" {if(debugl) printf("]"); yylval.str = strdup("]"); return R_SQ_BR;}
"(" {if(debugl) printf("("); yylval.str = strdup("("); return L_CURV_BR;}
")" {if(debugl) printf(")"); yylval.str = strdup(")"); return R_CURV_BR;}
"{" {if(debugl) printf("{"); yylval.str = strdup("{"); return L_FLR_BR;}
"}" {if(debugl) printf("}"); yylval.str = strdup("}"); return R_FLR_BR;}

";" {if(debugl) printf(";"); yylval.str = strdup(";"); return SEMI_COLON;}
"," {if(debugl) printf(","); yylval.str = strdup(","); return COMMA;}

"&&" {if(debugl) printf("&&"); yylval.str = strdup("&&"); return LOGICAL_AND;}
"||" {if(debugl) printf("||"); yylval.str = strdup("||"); return LOGICAL_OR;}
"!=" {if(debugl) printf("!="); yylval.str = strdup("!="); return NOT_EQUALS;}
"!" {if(debugl) printf("!"); yylval.str = strdup("!"); return LOGICAL_NOT;}
"<=" {if(debugl) printf("<="); yylval.str = strdup("<="); return LESS_EQUAL;}
"+" {if(debugl) printf("+"); yylval.str = strdup("+"); return ADD;}
"-" {if(debugl) printf("-"); yylval.str = strdup("-"); return SUBTRACT;}
"*" {if(debugl) printf("*"); yylval.str = strdup("*"); return MULTIPLY;}
"/" {if(debugl) printf("/"); yylval.str = strdup("/"); return DIVIDE;}
"=" {if(debugl) printf("="); yylval.str = strdup("="); return EQUALS;}
"length" {if(debugl) printf("length"); yylval.str = strdup("length"); return LENGTH;}
"." {if(debugl) printf("."); yylval.str = strdup("."); return DOT;}

"0" {yylval.str = strdup(yytext); if(debugl) printf("%s", yytext); return INTEGER_LITERAL;}
[1-9][0-9]* {yylval.str = strdup(yytext); if(debugl) printf("%s", yytext); return INTEGER_LITERAL;}
[a-zA-Z_][a-zA-Z0-9_]* {yylval.str = strdup(yytext); if(debugl) printf("%s", yytext); return IDENTIFIER;}

. {if(debugl) printf("%s", yytext); yyerror("Unknown character");}


%%

int yywrap() {return 1;}