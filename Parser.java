//> Parsing Expressions parser

//> Statements and State parser-imports
import java.util.ArrayList;
//< Statements and State parser-imports
//> Control Flow import-arrays
import java.util.Arrays;
//< Control Flow import-arrays
import java.util.List;

class Parser {
//> parse-error
  private static class ParseError extends RuntimeException {}

//< parse-error
  private final List<Token> tokens;

  // Chapter 8, CHALLENGE 1
  private final boolean replMode;
  private int loopDepth = 0; 
  private int current = 0;

  Parser(List<Token> tokens) {
    this(tokens, false);
  }

  Parser(List<Token> tokens, boolean replMode) {
    this.tokens = tokens;
    this.replMode = replMode;
  }

/* Parsing Expressions parse < Statements and State parse
  Expr parse() {
    try {
      return expression();
    } catch (ParseError error) {
      return null;
    }
  }
*/
//> Statements and State parse
  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
/* Statements and State parse < Statements and State parse-declaration
      statements.add(statement());
*/
//> parse-declaration
      statements.add(declaration());
//< parse-declaration
    }

    return statements; // [parse-error-handling]
  }
//< Statements and State parse
//> expression
  private Expr expression() {
/* Parsing Expressions expression < Statements and State expression
    return equality();
*/
//> Statements and State expression

// CHALLENGE 1: Comma expressions have lower precedence
    return comma();
//< Statements and State expression
  }

// CHALLENGE 1: Added comma expression parsing
// Comma is left-associative because each new expression
// is combined with the expression parsed so far
  private Expr comma() {
    // CHALLENGE 1: Detect comma without left-hand operand
    if (match(TokenType.COMMA)){
      error(previous(), "Expect expression before ','");
      conditional(); // Parse the right-hand side to continue parsing
      return null; // Return null to indicate an error
    }

    Expr expr = assignment();

    while (match(TokenType.COMMA)) {
        Token operator = previous();
        Expr right = assignment();

        expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
}

// CHALLENGE 2: Added support for the conditional/ternary operator.
private Expr conditional() {
    Expr expr = or();

    if (match(TokenType.QUESTION)) {
        Expr thenBranch = expression();

        consume(TokenType.COLON, "Expect ':' after then branch.");

        Expr elseBranch = conditional();

        expr = new Expr.Ternary(expr, thenBranch, elseBranch);
    }

    return expr;
}

//< expression
//> Statements and State declaration
  private Stmt declaration() {
    try {
//> Classes match-class
      if (match(TokenType.CLASS)) return classDeclaration();
//< Classes match-class
//> Functions match-fun
      if (match(TokenType.FUN)) return function("function");
//< Functions match-fun
      if (match(TokenType.VAR)) return varDeclaration();

      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }
//< Statements and State declaration
//> Classes parse-class-declaration
  private Stmt classDeclaration() {
    Token name = consume(TokenType.IDENTIFIER, "Expect class name.");
//> Inheritance parse-superclass

    Expr.Variable superclass = null;
    if (match(TokenType.LESS)) {
      consume(TokenType.IDENTIFIER, "Expect superclass name.");
      superclass = new Expr.Variable(previous());
    }

//< Inheritance parse-superclass
    consume(TokenType.LEFT_BRACE, "Expect '{' before class body.");

    List<Stmt.Function> methods = new ArrayList<>();
    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
      methods.add(function("method"));
    }

    consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");

/* Classes parse-class-declaration < Inheritance construct-class-ast
    return new Stmt.Class(name, methods);
*/
//> Inheritance construct-class-ast
    return new Stmt.Class(name, superclass, methods);
//< Inheritance construct-class-ast
  }
//< Classes parse-class-declaration
//> Statements and State parse-statement
  private Stmt statement() {
//> Control Flow match-for
    if (match(TokenType.FOR)) return forStatement();
//< Control Flow match-for
//> Control Flow match-if
    if (match(TokenType.IF)) return ifStatement();
//< Control Flow match-if
    if (match(TokenType.PRINT)) return printStatement();
//> Functions match-return
    if (match(TokenType.RETURN)) return returnStatement();
//< Functions match-return
//> Control Flow match-while
    if (match(TokenType.WHILE)) return whileStatement();
//< Control Flow match-while
//> parse-block
    if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());
//< break state
    if (match(TokenType.BREAK)) return breakStatement();

    return expressionStatement();
  }
//< Statements and State parse-statement
//> Control Flow for-statement
  private Stmt forStatement() {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");

/* Control Flow for-statement < Control Flow for-initializer
    // More here...
*/
//> for-initializer
    Stmt initializer;
    if (match(TokenType.SEMICOLON)) {
      initializer = null;
    } else if (match(TokenType.VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }
//< for-initializer
//> for-condition

    Expr condition = null;
    if (!check(TokenType.SEMICOLON)) {
      condition = expression();
    }
    consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");
//< for-condition
//> for-increment

    Expr increment = null;
    if (!check(TokenType.RIGHT_PAREN)) {
      increment = expression();
    }
    consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");
//< for-increment
//> for-body
    loopDepth++;
    try {
      Stmt body = statement();

      if (increment != null) {
        body = new Stmt.Block(
            Arrays.asList(body, new Stmt.Expression(increment)));
      }


//> for-desugar-increment
    if (increment != null) {
      body = new Stmt.Block(
          Arrays.asList(
              body,
              new Stmt.Expression(increment)));
    }

//< for-desugar-increment
//> for-desugar-condition
    if (condition == null) condition = new Expr.Literal(true);
    body = new Stmt.While(condition, body);

//< for-desugar-condition
//> for-desugar-initializer
    if (initializer != null) {
      body = new Stmt.Block(Arrays.asList(initializer, body));
    }

//< for-desugar-initializer
    return body;
  } finally {
    loopDepth--;
  }
//< for-body
  }
//< Control Flow for-statement
//> Control Flow if-statement
  private Stmt ifStatement() {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition."); // [parens]

    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (match(TokenType.ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }
//< Control Flow if-statement
//> Statements and State parse-print-statement
  private Stmt printStatement() {
    Expr value = expression();
    consume(TokenType.SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }
//< Statements and State parse-print-statement
//> Functions parse-return-statement
  private Stmt returnStatement() {
    Token keyword = previous();
    Expr value = null;
    if (!check(TokenType.SEMICOLON)) {
      value = expression();
    }

    consume(TokenType.SEMICOLON, "Expect ';' after return value.");
    return new Stmt.Return(keyword, value);
  }
//< Functions parse-return-statement
//> Statements and State parse-var-declaration
  private Stmt varDeclaration() {
    Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(TokenType.EQUAL)) {
      initializer = expression();
    }

    consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }
//< Statements and State parse-var-declaration
//> Control Flow while-statement
  private Stmt whileStatement() {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
    Expr condition = expression();
    consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");

    loopDepth++;
    try {
      Stmt body = statement();
      return new Stmt.While(condition, body);
    } finally {
      loopDepth--;
    }
  }
//< Control Flow while-statement
//> Statements and State parse-expression-statement
  private Stmt expressionStatement() {
    Expr expr = expression();

    // Chapter 8, CHALLENGE 1 REPL mode handling for bare expressions
    if (replMode && isAtEnd()) {
      // No semicolon, and nothing left to parse — they typed a bare
      // expression at the prompt, so show its value instead of
      // discarding it.
      return new Stmt.Print(expr);
    }

    consume(TokenType.SEMICOLON, "Expect ';' after expression.");
    return new Stmt.Expression(expr);
  }

  private Stmt breakStatement() {
    Token keyword = previous();
    if (loopDepth == 0) {
      error(keyword, "Can't use 'break' outside of a loop.");
    }
    consume(TokenType.SEMICOLON, "Expect ';' after 'break'.");
    return new Stmt.Break(keyword);
  } 


//< Statements and State parse-expression-statement
//> Functions parse-function
  private Stmt.Function function(String kind) {
    Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
//> parse-parameters
    consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
    List<Token> parameters = new ArrayList<>();
    if (!check(TokenType.RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "Can't have more than 255 parameters.");
        }

        parameters.add(
            consume(TokenType.IDENTIFIER, "Expect parameter name."));
      } while (match(TokenType.COMMA));
    }
    consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
//< parse-parameters
//> parse-body

    consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
    List<Stmt> body = block();
    return new Stmt.Function(name, parameters, body);
//< parse-body
  }
//< Functions parse-function
//> Statements and State block
  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
    return statements;
  }
//< Statements and State block
//> Statements and State parse-assignment
  private Expr assignment() {
/* Statements and State parse-assignment < Control Flow or-in-assignment
    Expr expr = equality();
*/
    Expr expr = conditional();

    if (match(TokenType.EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, value);
//> Classes assign-set
      } else if (expr instanceof Expr.Get) {
        Expr.Get get = (Expr.Get)expr;
        return new Expr.Set(get.object, get.name, value);
//< Classes assign-set
      }

      error(equals, "Invalid assignment target."); // [no-throw]
    }

    return expr;
  }
//< Statements and State parse-assignment
//> Control Flow or
private Expr or() {
    // CHALLENGE 3: Detect "or" without a left-hand operand.
    if (match(TokenType.OR)) {
        error(previous(), "Expect expression.");
        and(); // Parse and discard the right-hand operand.
        return null;
    }

    Expr expr = and();

    while (match(TokenType.OR)) {
        Token operator = previous();
        Expr right = and();
        expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
}
//< Control Flow or
//> Control Flow and
private Expr and() {
    // CHALLENGE 3: Detect "and" without a left-hand operand.
    if (match(TokenType.AND)) {
        error(previous(), "Expect expression.");
        equality(); // Parse and discard the right-hand operand.
        return null;
    }

    Expr expr = equality();

    while (match(TokenType.AND)) {
        Token operator = previous();
        Expr right = equality();
        expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
}
//< Control Flow and
//> equality
private Expr equality() {
    // CHALLENGE 3: Detect equality operators without a left-hand operand.
    if (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
        error(previous(), "Expect expression.");
        comparison(); // Parse and discard the right-hand operand.
        return null;
    }

    Expr expr = comparison();

    while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
        Token operator = previous();
        Expr right = comparison();
        expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
}
//< equality
//> comparison
  private Expr comparison() {
    Expr expr = term();

    while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }
//< comparison
//> term
  private Expr term() {
    Expr expr = factor();

    while (match(TokenType.MINUS, TokenType.PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }
//< term
//> factor
  private Expr factor() {
    Expr expr = unary();

    while (match(TokenType.SLASH, TokenType.STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }
//< factor
//> unary
  private Expr unary() {
    if (match(TokenType.BANG, TokenType.MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

/* Parsing Expressions unary < Functions unary-call
    return primary();
*/
//> Functions unary-call
    return call();
//< Functions unary-call
  }
//< unary
//> Functions finish-call
  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (!check(TokenType.RIGHT_PAREN)) {
      do {
//> check-max-arity
        if (arguments.size() >= 255) {
          error(peek(), "Can't have more than 255 arguments.");
        }
//< check-max-arity
        arguments.add(expression());
      } while (match(TokenType.COMMA));
    }

    Token paren = consume(TokenType.RIGHT_PAREN,
                          "Expect ')' after arguments.");

    return new Expr.Call(callee, paren, arguments);
  }
//< Functions finish-call
//> Functions call
  private Expr call() {
    Expr expr = primary();

    while (true) { // [while-true]
      if (match(TokenType.LEFT_PAREN)) {
        expr = finishCall(expr);
//> Classes parse-property
      } else if (match(TokenType.DOT)) {
        Token name = consume(TokenType.IDENTIFIER,
            "Expect property name after '.'.");
        expr = new Expr.Get(expr, name);
//< Classes parse-property
      } else {
        break;
      }
    }

    return expr;
  }
//< Functions call
//> primary
  private Expr primary() {
    if (match(TokenType.FALSE)) return new Expr.Literal(false);
    if (match(TokenType.TRUE)) return new Expr.Literal(true);
    if (match(TokenType.NIL)) return new Expr.Literal(null);

    if (match(TokenType.NUMBER, TokenType.STRING)) {
      return new Expr.Literal(previous().literal);
    }
//> Inheritance parse-super

    if (match(TokenType.SUPER)) {
      Token keyword = previous();
      consume(TokenType.DOT, "Expect '.' after 'super'.");
      Token method = consume(TokenType.IDENTIFIER,
          "Expect superclass method name.");
      return new Expr.Super(keyword, method);
    }
//< Inheritance parse-super
//> Classes parse-this

    if (match(TokenType.THIS)) return new Expr.This(previous());
//< Classes parse-this
//> Statements and State parse-identifier

    if (match(TokenType.IDENTIFIER)) {
      return new Expr.Variable(previous());
    }
//< Statements and State parse-identifier

    if (match(TokenType.LEFT_PAREN)) {
      Expr expr = expression();
      consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }
//> primary-error

    throw error(peek(), "Expect expression.");
//< primary-error
  }
//< primary
//> match
  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }
//< match
//> consume
  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();

    throw error(peek(), message);
  }
//< consume
//> check
  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }
//< check
//> advance
  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }
//< advance
//> utils
  private boolean isAtEnd() {
    return peek().type == TokenType.EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }
//< utils
//> error
  private ParseError error(Token token, String message) {
    Lox.error(token.line, message);
    return new ParseError();
  }
//< error
//> synchronize
  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == TokenType.SEMICOLON) return;

      switch (peek().type) {
        case TokenType.CLASS:
        case TokenType.FUN:
        case TokenType.VAR:
        case TokenType.FOR:
        case TokenType.IF:
        case TokenType.WHILE:
        case TokenType.PRINT:
        case TokenType.RETURN:
          return;
      }

      advance();
    }
  }
//< synchronize
}
