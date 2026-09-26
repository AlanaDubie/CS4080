//> Functions break-exception
// Added for Chapter 9
class BreakException extends RuntimeException {
  BreakException() {
    super(null, null, false, false);
  }
}