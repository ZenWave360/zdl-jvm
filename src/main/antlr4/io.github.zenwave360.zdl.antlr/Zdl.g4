grammar Zdl;

@lexer::members {
	private int _currentRuleType = Token.INVALID_TYPE;

	public int getCurrentRuleType() {
		return _currentRuleType;
	}

	public void setCurrentRuleType(int ruleType) {
		this._currentRuleType = ruleType;
	}

    @Override
    public Token emit() {

        return super.emit();
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

// Keywords
CONFIG: 'config';
APIS: 'apis';
ASYNCAPI: 'asyncapi';
OPENAPI: 'openapi';
ENTITY: 'entity';
ENUM: 'enum';
INPUT: 'input';
OUTPUT: 'output';
EVENT: 'event';
RELATIONSHIP: 'relationship';
MANY_TO_MANY: 'ManyToMany';
MANY_TO_ONE: 'ManyToOne';
ONE_TO_MANY: 'OneToMany';
ONE_TO_ONE: 'OneToOne';
//fragment SERVICE_TOKEN: 'service';
//SERVICE: ~'@' SERVICE_TOKEN; // not starting with @
SERVICE: 'service';
WITH: 'with';
FOR: 'for';
WITH_EVENTS: 'withEvents';

// options with reserved tokens
fragment CONFIG_OPTION: '@config';
fragment APIS_OPTION: '@apis';
fragment ASYNCAPI_OPTION: '@asyncapi';
fragment OPENAPI_OPTION: '@openapi';
fragment ENTITY_OPTION: '@entity';
fragment SERVICE_OPTION: '@service';
fragment INPUT_OPTION: '@input';
fragment OUTPUT_OPTION: '@output';
fragment EVENT_OPTION: '@event';
fragment RELATIONSHIP_OPTION: '@relationship';
fragment ENUM_OPTION: '@enum';
fragment PAGINATED_OPTION: '@paginated';
RESERVED_OPTIONS: CONFIG_OPTION | APIS_OPTION | ASYNCAPI_OPTION | OPENAPI_OPTION | ENTITY_OPTION | SERVICE_OPTION | INPUT_OPTION | OUTPUT_OPTION | EVENT_OPTION | RELATIONSHIP_OPTION | ENUM_OPTION | PAGINATED_OPTION;

REQUIRED: 'required';
UNIQUE: 'unique';
MIN: 'min';
MAX: 'max';
MINLENGTH: 'minlength';
MAXLENGTH: 'maxlength';
PATTERN: 'pattern';
AT: '@';
ARRAY: '[]';
TRUE: 'true';
FALSE: 'false';
NULL: 'null';
EQUALS: '=';


fragment DIGIT : [0-9] ;

ID: [a-zA-Z_][a-zA-Z0-9_]*;
INT: DIGIT+ ;
NUMBER: DIGIT+ ([.] DIGIT+)? ;

LEGACY_CONSTANT: LEGACY_CONSTANT_NAME ' '* EQUALS ' '* INT;
LEGACY_CONSTANT_NAME: [A-Z0-9_]+;

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
zdl: global_javadoc? legacy_constants config? apis? (entity | enum | input | output | event | relationships | service | service_legacy)* EOF;
global_javadoc: JAVADOC;
javadoc: JAVADOC;
suffix_javadoc: JAVADOC;

legacy_constants: LEGACY_CONSTANT*;

config: CONFIG config_body;
config_body: LBRACE config_option* RBRACE;
config_option: option_name option_value;

apis: APIS apis_body;
apis_body: LBRACE api* RBRACE;
api: javadoc? annotations api_type (LPAREN api_role RPAREN)? api_name api_body;
api_type: ASYNCAPI | OPENAPI;
api_role: ID;
api_name: ID;
api_body: LBRACE api_configs RBRACE;
api_configs: (api_config)*;
api_config: option_name option_value;

// values
value: simple | object;
simple: ID | SINGLE_QUOTED_STRING | DOUBLE_QUOTED_STRING | INT | NUMBER | TRUE | FALSE | NULL;
pair: ID COLON value;
object: LBRACE pair (COMMA pair)* RBRACE;
array: LBRACK? value (COMMA value)* RBRACK?;

// @options
annotations: option*;
option: reserved_option (LPAREN option_value RPAREN)? | '@' option_name (LPAREN option_value RPAREN)?;
//reserved_option: CONFIG_OPTION | APIS_OPTION | OPENAPI_OPTION | ASYNCAPI_OPTION | ENTITY_OPTION | SERVICE_OPTION | INPUT_OPTION | OUTPUT_OPTION | EVENT_OPTION | RELATIONSHIP_OPTION | ENUM_OPTION | PAGINATED_OPTION;
reserved_option: RESERVED_OPTIONS;
option_name: ID;
option_value: value | array | object;

// entities
entity: javadoc? annotations ENTITY entity_definition entity_body;
entity_definition: entity_name entity_table_name?;
entity_name: ID;
entity_table_name: LPAREN ID RPAREN;
entity_body: LBRACE fields RBRACE;

fields: (field COMMA?)*;
field: javadoc? annotations field_name field_type entity_table_name? (field_validations)* suffix_javadoc? (nested_field)?;
nested_field: LBRACE (field)* RBRACE nested_field_validations*;
field_name: ID;
field_type: ID | ID ARRAY;
//field_validations: REQUIRED | UNIQUE | min_validation | max_validation | minlength_validation | maxlength_validation | pattern_validation;
field_validations: field_validation_name (LPAREN field_validation_value RPAREN)?;
field_validation_name: REQUIRED | UNIQUE | MIN | MAX | MINLENGTH | MAXLENGTH | PATTERN;
field_validation_value: INT | ID | PATTERN_REGEX;
nested_field_validations: nested_field_validation_name (LPAREN nested_field_validation_value RPAREN)?;
nested_field_validation_name: REQUIRED | UNIQUE;
nested_field_validation_value: INT | ID | PATTERN_REGEX;

// enums
enum: javadoc? annotations ENUM enum_name enum_body;
enum_name: ID;
enum_body: LBRACE (enum_value)* RBRACE;
enum_value: javadoc? enum_value_name (LPAREN enum_value_value RPAREN)? suffix_javadoc? COMMA?;
enum_value_name: ID;
enum_value_value: value;

// inputs
input: javadoc? annotations INPUT input_name LBRACE fields RBRACE;
input_name: ID;

// outputs
output: javadoc? annotations OUTPUT output_name LBRACE fields RBRACE;
output_name: ID;

// events
event: javadoc? annotations EVENT event_name (LPAREN event_channel RPAREN)? LBRACE fields RBRACE;
event_name: ID;
event_channel: ID;

// relationships
relationships: RELATIONSHIP relationship_type  LBRACE relationship* RBRACE;
relationship_type: MANY_TO_MANY | MANY_TO_ONE| ONE_TO_MANY | ONE_TO_ONE;
relationship: relationship_from 'to' relationship_to;
relationship_from: javadoc? annotations relationship_definition;
relationship_to: javadoc? annotations relationship_definition;
relationship_definition: relationship_entity_name (LBRACE relationship_field_name relationship_description_field? RBRACE)?;
relationship_entity_name: ID;
relationship_field_name: ID;
relationship_description_field: LPAREN ID RPAREN;


// services
service: javadoc? annotations SERVICE ID FOR LPAREN service_aggregates RPAREN LBRACE service_method* RBRACE;
service_aggregates: ID (COMMA ID)*;
service_method: javadoc? annotations service_method_name LPAREN service_method_parameter_id? COMMA? service_method_parameter? RPAREN service_method_return? service_method_with_events? suffix_javadoc?;
service_method_name: ID;
service_method_parameter_id: 'id';
service_method_parameter: ID;
service_method_return: ID | ID ARRAY;
service_method_with_events: WITH_EVENTS (service_method_events)*;
service_method_events: service_method_event | service_method_events_or;
service_method_event: ID;
service_method_events_or: LPAREN ID (OR ID)* RPAREN | LBRACK ID (OR ID)* RBRACK;

service_legacy: SERVICE service_aggregates 'with' ID;


