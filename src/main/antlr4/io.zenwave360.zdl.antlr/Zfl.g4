grammar Zfl;

@lexer::members {
	private int _currentRuleType = Token.INVALID_TYPE;

	public int getCurrentRuleType() {
		return _currentRuleType;
	}

	public void setCurrentRuleType(int ruleType) {
		this._currentRuleType = ruleType;
	}
}

@parser::members {
    public Token match(int ttype) throws RecognitionException {
        try { // hack to not parse suffix javadoc after new lines (hidden tokens)
            if(getRuleContext() instanceof Suffix_javadocContext) {
                // System.out.println("RULE_suffix_javadoc");
                int currentTokenIndex = getCurrentToken().getTokenIndex();
                var prevToken = getTokenStream().get(currentTokenIndex - 1);
                if(prevToken.getText().contains("\n")) {
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.match(ttype);
    }
}

LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
LBRACK: '[';
RBRACK: ']';
OR: '|';
COMMA: ',';
COLON: ':';
TRUE: 'true';
FALSE: 'false';
NULL: 'null';
EQUALS: '=';
ARRAY: '[]';
OPTIONAL: '?';

// Keywords
IMPORT: 'import';
CONFIG: 'config';
// Flow Keywords
FLOW: 'flow';
SYSTEMS: 'systems';
ZDL: 'zdl';
SERVICE: 'service';
COMMANDS: 'commands';
EVENTS: 'events';
START: 'start';
WHEN: 'when';
COMMAND: 'command';
EVENT: 'event';
IF: 'if';
ELSE: 'else';
POLICY: 'policy';
END: 'end';
COMPLETED: 'completed';
SUSPENDED: 'suspended';
CANCELLED: 'cancelled';
AND: 'and';

// field validators
REQUIRED: 'required';
UNIQUE: 'unique';
MIN: 'min';
MAX: 'max';
MINLENGTH: 'minlength';
MAXLENGTH: 'maxlength';
EMAIL: 'email';
PATTERN: 'pattern';
OPTION_NAME: '@' [a-zA-Z_][a-zA-Z0-9_]*;

fragment DIGIT : [0-9] ;
ID: [a-zA-Z_][a-zA-Z0-9_.]*;
POLICY_ID: [a-zA-Z_][a-zA-Z0-9_-]*;
INT: DIGIT+ ;
NUMBER: DIGIT+ ([.] DIGIT+)? ;

// Comments
//SUFFIX_JAVADOC: {getCharPositionInLine() > 10}? '/**' .*? '*/';
//SUFFIX_JAVADOC: '/***' .*? '*/';
JAVADOC: '/**' .*? '*/';
LINE_COMMENT : '//' .*? '\r'? '\n' -> channel(HIDDEN) ; // Match "//" stuff '\n'
COMMENT : '/*' .*? '*/' -> channel(HIDDEN) ; // Match "/*" stuff "*/"

DOUBLE_QUOTED_STRING :  '"' (ESC | ~["\\])* '"' ;
SINGLE_QUOTED_STRING :  '\'' (ESC | ~['\\])* '\'' ;
fragment ESC :   '\\' ['"\\/bfnrt] ;

// Whitespace
WS: [ \t\r\n]+ -> channel(HIDDEN);

PATTERN_REGEX: '/' .*? '/' ; // TODO: improve regex

/** "catch all" rule for any char not matche in a token rule of your
 *  grammar. Lexers in Intellij must return all tokens good and bad.
 *  There must be a token to cover all characters, which makes sense, for
 *  an IDE. The parser however should not see these bad tokens because
 *  it just confuses the issue. Hence, the hidden channel.
 */
ERRCHAR: . -> channel(HIDDEN);

// Rules
zfl: import_* config? flow* EOF;

import_: '@import' LPAREN (import_value | import_key COLON import_value) RPAREN;
import_key: ID;
import_value: string;
global_javadoc: JAVADOC;
javadoc: JAVADOC;
suffix_javadoc: JAVADOC;

// values
keyword: ID | IMPORT | CONFIG | FLOW | SYSTEMS | ZDL | SERVICE | COMMANDS | EVENTS | START | WHEN | COMMAND | EVENT | IF | ELSE | POLICY | END | COMPLETED | SUSPENDED | CANCELLED | AND | REQUIRED | UNIQUE | MIN | MAX | MINLENGTH | MAXLENGTH | EMAIL | PATTERN;

//complex_value: value | array | object;
//value: simple | object;
//string: keyword | SINGLE_QUOTED_STRING | DOUBLE_QUOTED_STRING;
//simple: keyword | SINGLE_QUOTED_STRING | DOUBLE_QUOTED_STRING | INT | NUMBER | TRUE | FALSE | NULL;
//pair: keyword COLON value;
//object: LBRACE pair (COMMA pair)* RBRACE;
//array: LBRACK? value (COMMA value)* RBRACK?;

complex_value : value | array_plain | pairs;
value: object| array | simple;
string: keyword | SINGLE_QUOTED_STRING | DOUBLE_QUOTED_STRING;
simple: ID | SINGLE_QUOTED_STRING | DOUBLE_QUOTED_STRING | INT | NUMBER | TRUE | FALSE | NULL | keyword;
object: LBRACE pair (COMMA pair)* RBRACE
      | LBRACE RBRACE;
pair: string COLON value;
array: LBRACK value (COMMA value)* RBRACK
     | LBRACK RBRACK;
array_plain: simple (COMMA simple)*;
pairs: pair (COMMA pair)*;

fields: (field COMMA?)*;
field: javadoc? annotations field_name field_type suffix_javadoc?;
field_name: keyword;
field_type: ID | ID ARRAY;

config: global_javadoc? CONFIG config_body;
config_body: LBRACE config_option* RBRACE;
config_option: field_name complex_value;

// @options
annotations: option*;
option: option_name (LPAREN option_value RPAREN)?; // (LPAREN option_value RPAREN)? | '@' option_name (LPAREN option_value RPAREN)?;
option_name: OPTION_NAME;
option_value: complex_value;

// flows
flow: javadoc? annotations FLOW flow_name LBRACE flow_body RBRACE;
flow_name: ID;
flow_body: (flow_systems | flow_start | flow_when | flow_end)*;

// systems block
flow_systems: javadoc? annotations SYSTEMS LBRACE flow_system* RBRACE;
flow_system: javadoc? annotations flow_system_name LBRACE flow_system_body RBRACE;
flow_system_name: ID;
flow_system_body: flow_system_zdl? flow_system_services flow_system_events?;
flow_system_zdl: ZDL COLON? string;
flow_system_services: flow_system_service*;
flow_system_service: SERVICE flow_system_service_name? LBRACE flow_system_service_body RBRACE;
flow_system_service_name: ID;
flow_system_service_body: COMMANDS COLON flow_system_service_command_list;
flow_system_service_command_list: ID (COMMA ID)*;
flow_system_events: EVENTS COLON flow_system_event_list;
flow_system_event_list: ID (COMMA ID)*;

// start events
flow_start: javadoc? annotations START flow_start_name LBRACE fields RBRACE;
flow_start_name: ID;

// when blocks
flow_when: javadoc? annotations WHEN flow_when_trigger LBRACE flow_when_body RBRACE;
flow_when_trigger: flow_when_event_trigger (AND flow_when_event_trigger)*;
flow_when_event_trigger: ID;
flow_when_body: (flow_when_command | flow_when_event | flow_when_if | flow_when_policy)*;
flow_when_command: COMMAND flow_command_name;
flow_command_name: ID;
flow_when_event: EVENT flow_event_name;
flow_event_name: ID;
flow_when_if: IF string LBRACE flow_when_body RBRACE flow_when_else_if* flow_when_else?;
flow_when_else_if: ELSE IF string LBRACE flow_when_body RBRACE;
flow_when_else: ELSE LBRACE flow_when_body RBRACE;
flow_when_policy: POLICY string;

// end block
flow_end: javadoc? annotations END LBRACE flow_end_outcomes RBRACE;
flow_end_outcomes: flow_end_completed? flow_end_suspended? flow_end_cancelled?;
flow_end_completed: COMPLETED COLON flow_end_outcome_list;
flow_end_suspended: SUSPENDED COLON flow_end_outcome_list;
flow_end_cancelled: CANCELLED COLON flow_end_outcome_list;
flow_end_outcome_list: flow_end_outcome_event (COMMA flow_end_outcome_event)*;
flow_end_outcome_event: ID;
