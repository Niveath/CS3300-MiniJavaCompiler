%{
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

extern int yylex();
void yyerror(char* s);

struct Node {
    char* token;
    struct Node* next;
};

struct Macro {
    char* name;
    int numArgs;
    struct Node* args;
    int* argUsage;
    int numTokens;
    struct Node* body;
};

struct Macro macroExpressions[100];
struct Macro macroStatements[100];

int numMacroExpressions = 0;
int numMacroStatements = 0;

int debugy = 0;

char* keywords[] = {"class", "public", "static", "void", "main", "String", "extends", "return", "do", "while", "if", "else", "true", "false", "this", "new", "int", "boolean", "#define", "length"};


struct Node* createNode(char* str) {
    struct Node* newNode = (struct Node*) malloc(sizeof(struct Node));
    newNode->token = strdup(str);
    newNode->next = NULL;

    return newNode;
}


int countArgs (struct Node* head) {
    if(head == NULL) return 0;

    int count = 0;
    struct Node* temp = head;
    while(temp != NULL) {
        count++;
        // printf("%d: %s\n", count, temp->token);
        temp = temp->next;
    }

    return count;
}


char** parseMacroArgs(struct Node* head, int numArgs) {
    if(head == NULL) return NULL;

    char** argTokens = (char**) malloc(numArgs * sizeof(char*));
    
    struct Node* temp = head;
    int i = 0;
    while(temp != NULL) {
        argTokens[i++] = strdup(temp->token);
        temp = temp->next;
    }

    return argTokens;
}


struct Node* createCopy(struct Node* head) {
    struct Node* copy = NULL;
    if(head == NULL) return copy;

    struct Node* temp = head;
    struct Node* tail = NULL;

    while(temp != NULL) {
        struct Node* newNode = (struct Node*) malloc(sizeof(struct Node));
        newNode->token = strdup(temp->token);
        newNode->next = NULL;

        if(copy == NULL) {
            copy = newNode;
            tail = newNode;
        }
        else {
            tail->next = newNode;
            tail = tail->next;
        }

        temp = temp->next;
    }

    return copy;
}


void addMacro(int isExp, struct Node* name, struct Node* args, struct Node* body) {
    for(int i=0; i<numMacroExpressions; i++) {
        if(strcmp(macroExpressions[i].name, name->token) == 0) {
            yyerror("Macro redefinition is illegal");
        }
    }
    for(int i=0; i<numMacroStatements; i++) {
        if(strcmp(macroStatements[i].name, name->token) == 0) {
            yyerror("Macro redefinition is illegal");
        }
    }
    for(int i=0; i<20; i++) {
        if(strcmp(keywords[i], name->token) == 0) {
            yyerror("Macro name cannot be a keyword");
        }
    }

    if(isExp) {
        int* argUsage = NULL;

        struct Macro newMacroExpression;
        newMacroExpression.name = name->token;
        
        if(args == NULL) {
            newMacroExpression.numArgs = 0;
            newMacroExpression.args = NULL;
            newMacroExpression.argUsage = NULL;
        }
        else {
            int numArgs = countArgs(args);

            if(debugy) printf("Num args: %d\n", numArgs);

            newMacroExpression.numArgs = numArgs;

            newMacroExpression.args = args;

            argUsage = (int*) malloc(numArgs * sizeof(int));
            for(int i=0; i<numArgs; i++) argUsage[i] = 0;

            struct Node* temp = body;
            
            while(temp != NULL) {
                struct Node* temp2 = args;
                int i = 0;
                while(temp2 != NULL) {
                    if(strcmp(temp->token, temp2->token) == 0) {
                        argUsage[i]++;
                        break;
                    }
                    temp2 = temp2->next;
                    i++;
                }
                temp = temp->next;
            }
        }

        newMacroExpression.argUsage = argUsage;

        newMacroExpression.numTokens = countArgs(body);

        newMacroExpression.body = body;

        macroExpressions[numMacroExpressions++] = newMacroExpression;
        // printf("\nExpr, Name: %s, NumArgs:%d, NumTokens:%d\n", newMacroExpression.name, newMacroExpression.numArgs, newMacroExpression.numTokens);
    }
    else {
        int* argUsage = NULL;

        struct Macro newMacroStatement;
        newMacroStatement.name = name->token;

        if(args == NULL) {
            newMacroStatement.numArgs = 0;
        }
        else {
            int numArgs = countArgs(args);

            newMacroStatement.numArgs = numArgs;

            newMacroStatement.args = args;

            argUsage = (int*) malloc(numArgs * sizeof(int));
            for(int i=0; i<numArgs; i++) argUsage[i] = 0;

            struct Node* temp = body;
            
            while(temp != NULL) {
                struct Node* temp2 = args;
                int i = 0;
                while(temp2 != NULL) {
                    if(strcmp(temp->token, temp2->token) == 0) {
                        argUsage[i]++;
                        break;
                    }
                    temp2 = temp2->next;
                    i++;
                }
                temp = temp->next;
            }
        }

        newMacroStatement.argUsage = argUsage;

        newMacroStatement.numTokens = countArgs(body);

        newMacroStatement.body = body;

        macroStatements[numMacroStatements++] = newMacroStatement;
        // printf("\nStmt, Name: %s, NumArgs:%d, NumTokens:%d\n", newMacroStatement.name, newMacroStatement.numArgs, newMacroStatement.numTokens);
    }
}


struct Node* replaceMacro(int isExp, struct Node* name, struct Node* head) {
    int numArgs = countArgs(head);
    char** argTokens = parseMacroArgs(head, numArgs);

    if(isExp) {
        for(int i=0; i<numMacroExpressions; i++) {
            if(strcmp(macroExpressions[i].name, name->token) == 0) {
                if(macroExpressions[i].numArgs == 0 && numArgs == 0) {
                    return createCopy(macroExpressions[i].body);
                }
                else if(macroExpressions[i].numArgs == 0 && numArgs != 0) yyerror("Macro argument count does not match");
                else {
                    if(macroExpressions[i].numArgs != numArgs) yyerror("Macro argument count does not match");

                    struct Node* result = (struct Node*) malloc(sizeof(struct Node));
                    result->token = strdup("(");

                    struct Node* tail = result;

                    struct Node* temp = macroExpressions[i].body;
                    

                    char** argTokens = parseMacroArgs(head, numArgs);

                    while(temp != NULL) {
                        int isArg = 0;
                        struct Node* temp2 = macroExpressions[i].args;
                        int j = 0;
                        while(temp2 != NULL) {
                            if(strcmp(temp->token, temp2->token) == 0) {
                                struct Node* newNode = (struct Node*) malloc(sizeof(struct Node));

                                char* newToken = (char*) malloc((strlen(argTokens[j]) + 3) * sizeof(char));
                                strcat(newToken, "(");
                                strcat(newToken, argTokens[j]);
                                strcat(newToken, ")");
                                newNode->token = newToken;
                                newNode->next = NULL;

                                tail->next = newNode;
                                tail = tail->next;

                                isArg = 1;
                                break;
                            }
                            j++;
                            temp2 = temp2->next;
                        }
                        if(!isArg) {
                            struct Node* newNode = (struct Node*) malloc(sizeof(struct Node));
                            newNode->token = strdup(temp->token);
                            newNode->next = NULL;

                            tail->next = newNode;
                            tail = tail->next;
                        }
                        temp = temp->next;
                    }

                    struct Node* newNode = (struct Node*) malloc(sizeof(struct Node));
                    newNode->token = strdup(")");
                    newNode->next = NULL;
                    tail->next = newNode;

                    return result;
                }
            }
        }

        yyerror("Undefined macro");
    }
    else {
        for(int i=0; i<numMacroStatements; i++) {
            if(strcmp(macroStatements[i].name, name->token) == 0) {
                if(macroStatements[i].numArgs == 0 && numArgs == 0) {
                    return createCopy(macroStatements[i].body);
                }
                
                else if(macroStatements[i].numArgs == 0 && numArgs != 0) yyerror("Macro argument count does not match21");
                else {
                    if(macroStatements[i].numArgs != numArgs) {
                        printf("%s\n", macroStatements[i].name);
                        printf("%d %d\n", macroStatements[i].numArgs, numArgs);
                        yyerror("Macro argument count does not match22");
                    }

                    struct Node* result = (struct Node*) malloc(sizeof(struct Node));
                    struct Node* tail = NULL;

                    struct Node* temp = macroExpressions[i].body;

                    char** argTokens = parseMacroArgs(head, numArgs);

                    while(temp != NULL) {
                        int isArg = 0;

                        struct Node* temp2 = macroExpressions[i].args;
                        int j = 0;
                        while(temp2 != NULL) {
                            if(strcmp(temp->token, temp2->token) == 0) {
                                struct Node* newNode = (struct Node*) malloc(sizeof(struct Node));
                                newNode->token = strdup(argTokens[j]);
                                newNode->next = NULL;

                                if(result == NULL) {
                                    result = newNode;
                                    tail = newNode;
                                }
                                else {
                                    tail->next = newNode;
                                    tail = tail->next;
                                }

                                isArg = 1;
                                break;
                            }
                            j++;
                            temp2 = temp2->next;
                        }
                        if(!isArg) {
                            struct Node* newNode = (struct Node*) malloc(sizeof(struct Node));
                            newNode->token = strdup(temp->token);
                            newNode->next = NULL;

                            tail->next = newNode;
                            tail = tail->next;
                        }
                        temp = temp->next;
                    }

                    return result;
                }
            }

        }

        yyerror("Undefined macro");
    }

    return NULL;
}


struct Node* concatenate(int numNodes, struct Node* nodes[]) {
    struct Node* head = nodes[0];

    struct Node* temp = head;
    while(temp->next != NULL) temp = temp->next;

    for(int i=1; i<numNodes; i++) {
        if(strcmp(nodes[i]->token, "") == 0) continue;
        temp->next = nodes[i];
        while(temp->next != NULL) temp = temp->next;
    }
    temp->next = NULL;

    return head;
}

void printLinkedList(struct Node* head) {
    while(head != NULL) {
        printf("%s ", head->token);
        head = head->next;
    }
}

%}

%union {
    char* str;
    struct Node* node;
}

/*----------------TOKENS-FOR-KEYWORDS--------------------*/
%token<str> CLASS PUBLIC STATIC VOID MAIN
%token<str> STRING_DECL PRINTLN EXTENDS RETURN
%token<str> DO WHILE IF ELSE
%token<str> TRUE_TOKEN FALSE_TOKEN THIS NEW
%token<str> INT BOOLEAN
%token<str> HASH_DEFINE
%token<str> L_SQ_BR R_SQ_BR L_CURV_BR R_CURV_BR L_FLR_BR R_FLR_BR
%token<str> SEMI_COLON COMMA

/*----------------TOKENS-FOR-OPERATORS--------------------*/
%token<str> LOGICAL_AND LOGICAL_OR LOGICAL_NOT
%token<str> NOT_EQUALS LESS_EQUAL
%token<str> ADD SUBTRACT MULTIPLY DIVIDE EQUALS
%token<str> LENGTH DOT

/*----------------TOKENS-FOR-LITERALS---------------------*/
%token<str> IDENTIFIER INTEGER_LITERAL


/*----------------NONTERMINALS-FOR-TERMINALS---------------------*/
%type<node> Class Public Static Void Main
%type<node> String_Decl Println Extends Return
%type<node> Do While If Else
%type<node> True_Token False_Token This New
%type<node> Int Boolean
%type<node> Hash_Define
%type<node> L_Sq_Br R_Sq_Br L_Curv_Br R_Curv_Br L_Flr_Br R_Flr_Br
%type<node> Semi_Colon Comma
%type<node> Logical_And Logical_Or Logical_Not
%type<node> Not_Equals Less_Equal
%type<node> Add Subtract Multiply Divide Equals
%type<node> Length Dot
%type<node> Identifier Integer_Literal

/*----------------TYPE-DECLARATION-FOR-NON-TERMINALS-------*/

%type<node> goal MacroDefinition MainClass TypeDeclaration MethodDeclaration
%type<node> MacroDefinitionExpression MacroDefinitionStatement
%type<node> IdentifierList CommaSeparatedIdentifierList ArgumentList StatementList 
%type<node> CommaSeparatedExpressionList
%type<node> Statement Expression PrimaryExpression
%type<node> Type Integer

%start goal

%%

goal: MacroDefinition MainClass TypeDeclaration 
        { 
            struct Node* arr[] = {$2, $3}; 
            $$ = concatenate(2, arr); 
            printLinkedList($$);
        }
;


MacroDefinition : {}
                | MacroDefinitionExpression MacroDefinition {}
                | MacroDefinitionStatement MacroDefinition {}
;


MacroDefinitionExpression : Hash_Define Identifier L_Curv_Br R_Curv_Br L_Curv_Br Expression R_Curv_Br
                            {
                                addMacro(1, $2, NULL, $6);
                            }
                | Hash_Define Identifier L_Curv_Br Identifier CommaSeparatedIdentifierList R_Curv_Br L_Curv_Br Expression R_Curv_Br
                { 
                    struct Node* arr[] = {$4, $5};

                    addMacro(1, $2, concatenate(2, arr), $8);
                }
;


MacroDefinitionStatement : Hash_Define Identifier L_Curv_Br R_Curv_Br L_Flr_Br StatementList R_Flr_Br
                { 
                    addMacro(0, $2, NULL, $6);
                }
                | Hash_Define Identifier L_Curv_Br Identifier CommaSeparatedIdentifierList R_Curv_Br L_Flr_Br StatementList R_Flr_Br
                {
                    struct Node* arr[] = {$4, $5};
                    addMacro(0, $2, concatenate(2, arr), $8);
                }
;


CommaSeparatedIdentifierList : { $$ = createNode(""); }
                | Comma Identifier CommaSeparatedIdentifierList { struct Node* arr[] = {$2, $3}; $$ = concatenate(2, arr); }


MainClass :     Class Identifier L_Flr_Br Public Static Void Main L_Curv_Br String_Decl L_Sq_Br R_Sq_Br Identifier R_Curv_Br
                L_Flr_Br Println L_Curv_Br Expression R_Curv_Br Semi_Colon R_Flr_Br R_Flr_Br 
                {
                    struct Node* arr[] = {$1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20, $21};
                    $$ = concatenate(21, arr);
                }
;


TypeDeclaration : { $$ = createNode(""); }
                | Class Identifier L_Flr_Br IdentifierList MethodDeclaration R_Flr_Br TypeDeclaration
                {
                    struct Node* arr[] = {$1, $2, $3, $4, $5, $6, $7};
                    $$ = concatenate(7, arr);
                }
                | Class Identifier Extends Identifier L_Flr_Br IdentifierList MethodDeclaration R_Flr_Br TypeDeclaration
                {
                    struct Node* arr[] = {$1, $2, $3, $4, $5, $6, $7, $8, $9};
                    $$ = concatenate(9, arr);
                }
;


MethodDeclaration : { $$ = createNode(""); }
                | Public Type Identifier L_Curv_Br R_Curv_Br L_Flr_Br IdentifierList StatementList Return Expression Semi_Colon R_Flr_Br MethodDeclaration 
                {
                    struct Node* arr[] = {$1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13};
                    $$ = concatenate(13, arr);
                }
                | Public Type Identifier L_Curv_Br Type Identifier ArgumentList R_Curv_Br L_Flr_Br IdentifierList StatementList Return Expression Semi_Colon R_Flr_Br MethodDeclaration
                {
                    struct Node* arr[] = {$1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16};
                    $$ = concatenate(16, arr);
                }
;


ArgumentList : { $$ = createNode(""); }
                | Comma Type Identifier ArgumentList 
                {
                    struct Node* arr[] = {$1, $2, $3, $4};
                    $$ = concatenate(4, arr);
                }
;


IdentifierList : { $$ = createNode(""); }
                | IdentifierList Type Identifier Semi_Colon  
                {
                    struct Node* arr[] = {$1, $2, $3, $4};
                    $$ = concatenate(4, arr);
                }
;


Type :            Int L_Sq_Br R_Sq_Br { struct Node* arr[] = {$1, $2, $3}; $$ = concatenate(3, arr); }
                | Boolean { $$ = $1; }
                | Int { $$ = $1; }
                | Identifier { $$ = $1; }
;

StatementList : { $$ = createNode(""); }
                | Statement StatementList { struct Node* arr[] = {$1, $2}; $$ = concatenate(2, arr); }
;


Statement :       L_Flr_Br StatementList R_Flr_Br { struct Node* arr[] = {$1, $2, $3}; $$ = concatenate(3, arr); }
                | Println L_Curv_Br Expression R_Curv_Br Semi_Colon 
                { 
                    struct Node* arr[] = {$1, $2, $3, $4, $5}; 
                    $$ = concatenate(5, arr); 
                }
                | Identifier Equals Expression Semi_Colon 
                { 
                    struct Node* arr[] = {$1, $2, $3, $4}; 
                    $$ = concatenate(4, arr); 
                }
                | Identifier L_Sq_Br Expression R_Sq_Br Equals Expression Semi_Colon 
                { 
                    struct Node* arr[] = {$1, $2, $3, $4, $5, $6, $7}; 
                    $$ = concatenate(7, arr); 
                }
                | If L_Curv_Br Expression R_Curv_Br Statement Else Statement 
                { 
                    struct Node* arr[] = {$1, $2, $3, $4, $5, $6, $7}; 
                    $$ = concatenate(7, arr); 
                }
                | If L_Curv_Br Expression R_Curv_Br Statement
                {
                    struct Node* arr[] = {$1, $2, $3, $4, $5};
                    $$ = concatenate(5, arr);
                }
                | Do Statement While L_Curv_Br Expression R_Curv_Br Semi_Colon 
                { 
                    struct Node* arr[] = {$1, $2, $3, $4, $5, $6, $7}; 
                    $$ = concatenate(7, arr); 
                }
                | While L_Curv_Br Expression R_Curv_Br Statement
                {
                    struct Node* arr[] = {$1, $2, $3, $4, $5};
                    $$ = concatenate(5, arr);
                }
                | Identifier L_Curv_Br R_Curv_Br Semi_Colon
                {
                    $$ = replaceMacro(0, $1, NULL);
                }
                | Identifier L_Curv_Br Expression CommaSeparatedExpressionList R_Curv_Br Semi_Colon
                {
                    struct Node* arr[] = {$3, $4};
                    $$ = replaceMacro(0, $1, concatenate(2, arr));
                }
;
            
Expression:     PrimaryExpression Logical_And PrimaryExpression { struct Node* arr[] = {$1, $2, $3}; $$ = concatenate(3, arr); }
                | PrimaryExpression Logical_Or PrimaryExpression { struct Node* arr[] = {$1, $2, $3}; $$ = concatenate(3, arr); }
                | PrimaryExpression Not_Equals PrimaryExpression { struct Node* arr[] = {$1, $2, $3}; $$ = concatenate(3, arr); }
                | PrimaryExpression Less_Equal PrimaryExpression { struct Node* arr[] = {$1, $2, $3}; $$ = concatenate(3, arr); }
                | PrimaryExpression Add PrimaryExpression { struct Node* arr[] = {$1, $2, $3}; $$ = concatenate(3, arr); }
                | PrimaryExpression Subtract  PrimaryExpression { struct Node* arr[] = {$1, $2, $3}; $$ = concatenate(3, arr); }
                | PrimaryExpression Multiply PrimaryExpression { struct Node* arr[] = {$1, $2, $3}; $$ = concatenate(3, arr); }
                | PrimaryExpression Divide PrimaryExpression { struct Node* arr[] = {$1, $2, $3}; $$ = concatenate(3, arr); }
                | PrimaryExpression L_Sq_Br PrimaryExpression R_Sq_Br { struct Node* arr[] = {$1, $2, $3, $4}; $$ = concatenate(4, arr); }
                | PrimaryExpression Dot Length { struct Node* arr[] = {$1, $2, $3}; $$ = concatenate(3, arr); }
                | PrimaryExpression { struct Node* arr[] = {$1}; $$ = concatenate(1, arr); }
                | PrimaryExpression Dot Identifier L_Curv_Br R_Curv_Br 
                {
                    struct Node* arr[] = {$1, $2, $3, $4, $5};
                    $$ = concatenate(5, arr);
                }
                | PrimaryExpression Dot Identifier L_Curv_Br Expression CommaSeparatedExpressionList R_Curv_Br
                {
                    struct Node* arr[] = {$1, $2, $3, $4, $5, $6, $7};
                    $$ = concatenate(7, arr);
                }
                | Identifier L_Curv_Br R_Curv_Br 
                {
                    $$ = replaceMacro(1, $1, NULL);
                }
                | Identifier L_Curv_Br Expression CommaSeparatedExpressionList R_Curv_Br
                {
                    struct Node* arr[] = {$3, $4};
                    $$ = replaceMacro(1, $1, concatenate(2, arr));
                }
;

CommaSeparatedExpressionList : { $$ = createNode(""); }
                | Comma Expression CommaSeparatedExpressionList 
                {
                    struct Node* arr[] = {$1, $2, $3};
                    $$ = concatenate(3, arr);
                }
;

PrimaryExpression : Integer { $$ = $1; }
                | True_Token { $$ = $1; }
                | False_Token { $$ = $1; }
                | Identifier { $$ = $1; }
                | This { $$ = $1; }
                | New Int L_Sq_Br Expression R_Sq_Br { struct Node* arr[] = {$1, $2, $3, $4, $5}; $$ = concatenate(5, arr); }
                | New Identifier L_Curv_Br R_Curv_Br { struct Node* arr[] = {$1, $2, $3, $4}; $$ = concatenate(4, arr); }
                | Logical_Not Expression { struct Node* arr[] = {$1, $2}; $$ = concatenate(2, arr); }
                | L_Curv_Br Expression R_Curv_Br { struct Node* arr[] = {$1, $2, $3}; $$ = concatenate(3, arr); }
;


Integer : Integer_Literal { $$ = $1; }
                | Add Integer_Literal { struct Node* arr[] = {$1, $2}; $$ = concatenate(2, arr); }
                | Subtract Integer_Literal { struct Node* arr[] = {$1, $2}; $$ = concatenate(2, arr); }
; 

/*-------------------- CREATING NODES FOR TOKENS ----------------------*/

Class : CLASS { $$ = createNode($1); }
;


Public : PUBLIC { $$ = createNode($1); }
;


Static : STATIC { $$ = createNode($1); }
;


Void : VOID { $$ = createNode($1); }
;


Main : MAIN { $$ = createNode($1); }
;


String_Decl : STRING_DECL { $$ = createNode($1); }
;


Println : PRINTLN { $$ = createNode($1); }
;


Extends : EXTENDS { $$ = createNode($1); }
;


Return : RETURN { $$ = createNode($1); }
;


Do : DO { $$ = createNode($1); }
;


While : WHILE { $$ = createNode($1); }
;


If : IF { $$ = createNode($1); }
;


Else : ELSE { $$ = createNode($1); }
;


True_Token : TRUE_TOKEN { $$ = createNode($1); }
;


False_Token : FALSE_TOKEN { $$ = createNode($1); }
;


This : THIS { $$ = createNode($1); }
;


New : NEW { $$ = createNode($1); }
;



Int : INT { $$ = createNode($1); }
;



Boolean : BOOLEAN { $$ = createNode($1); }
;



Hash_Define : HASH_DEFINE { $$ = createNode($1); }
;



L_Sq_Br : L_SQ_BR { $$ = createNode($1); }
;


R_Sq_Br : R_SQ_BR { $$ = createNode($1); }
;


L_Curv_Br : L_CURV_BR { $$ = createNode($1); }
;


R_Curv_Br : R_CURV_BR { $$ = createNode($1); }
;


L_Flr_Br : L_FLR_BR { $$ = createNode($1); }
;


R_Flr_Br : R_FLR_BR { $$ = createNode($1); }
;


Semi_Colon : SEMI_COLON { $$ = createNode($1); }
;


Comma : COMMA { $$ = createNode($1); }
;


Logical_And : LOGICAL_AND { $$ = createNode($1); }
;


Logical_Or : LOGICAL_OR { $$ = createNode($1); }
;


Logical_Not : LOGICAL_NOT { $$ = createNode($1); }
;


Not_Equals : NOT_EQUALS { $$ = createNode($1); }
;


Less_Equal : LESS_EQUAL { $$ = createNode($1); }
;


Add : ADD { $$ = createNode($1); }
;


Subtract : SUBTRACT { $$ = createNode($1); }
;


Multiply : MULTIPLY { $$ = createNode($1); }
;


Divide : DIVIDE { $$ = createNode($1); }
;


Equals : EQUALS { $$ = createNode($1); }
;


Length : LENGTH { $$ = createNode($1); }
;


Dot : DOT { $$ = createNode($1); }


Identifier : IDENTIFIER { $$ = createNode($1); }
;


Integer_Literal : INTEGER_LITERAL { $$ = createNode($1); }


%%

void yyerror(char* s) {
    if(debugy) printf("\n%s\n", s);
    printf("// Failed to parse macrojava code.");
    exit(1);
}

int main(int argc, char* argv[]) {
    yyparse();
}