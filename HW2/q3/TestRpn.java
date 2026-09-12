package HW2.q3;

public class TestRpn {

  public static void main(String[] args) {

    // Create tokens for + and -
    Token plus = new Token(TokenType.PLUS, "+", null, 1);
    Token minus = new Token(TokenType.MINUS, "-", null, 1);

    // 1 + 2
    Expr addition = new Expr.Binary(
        new Expr.Literal(1),
        plus,
        new Expr.Literal(2)
    );

    // 4 - 3
    Expr subtraction = new Expr.Binary(
        new Expr.Literal(4),
        minus,
        new Expr.Literal(3)
    );

    // (1 + 2) * (4 - 3)
    Token multiply = new Token(TokenType.STAR, "*", null, 1);

    Expr expression = new Expr.Binary(
        new Expr.Grouping(addition),
        multiply,
        new Expr.Grouping(subtraction)
    );

    RpnPrinter printer = new RpnPrinter();

    System.out.println(printer.print(expression));
  }
}