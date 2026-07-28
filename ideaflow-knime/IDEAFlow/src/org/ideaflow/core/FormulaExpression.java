package org.ideaflow.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Small deterministic expression language for objective and constraint formulas. It intentionally
 * has no scripting, reflection, file, or network access.
 */
public final class FormulaExpression {
  private interface Expression {
    double evaluate(Map<String, Double> variables);

    void variables(Set<String> output);
  }

  private record NumberExpression(double value) implements Expression {
    @Override
    public double evaluate(final Map<String, Double> variables) {
      return value;
    }

    @Override
    public void variables(final Set<String> output) {}
  }

  private record VariableExpression(String name) implements Expression {
    @Override
    public double evaluate(final Map<String, Double> variables) {
      final Double value = variables.get(name);
      if (value == null)
        throw new IllegalArgumentException("No value was provided for variable: " + name);
      return value;
    }

    @Override
    public void variables(final Set<String> output) {
      output.add(name);
    }
  }

  private record UnaryExpression(char operator, Expression value) implements Expression {
    @Override
    public double evaluate(final Map<String, Double> variables) {
      final double result = value.evaluate(variables);
      return operator == '-' ? -result : result;
    }

    @Override
    public void variables(final Set<String> output) {
      value.variables(output);
    }
  }

  private record BinaryExpression(char operator, Expression left, Expression right)
      implements Expression {
    @Override
    public double evaluate(final Map<String, Double> variables) {
      final double a = left.evaluate(variables);
      final double b = right.evaluate(variables);
      return switch (operator) {
        case '+' -> a + b;
        case '-' -> a - b;
        case '*' -> a * b;
        case '/' -> a / b;
        case '^' -> Math.pow(a, b);
        default -> throw new IllegalStateException("Unknown formula operator.");
      };
    }

    @Override
    public void variables(final Set<String> output) {
      left.variables(output);
      right.variables(output);
    }
  }

  private record FunctionExpression(String name, List<Expression> arguments) implements Expression {
    @Override
    public double evaluate(final Map<String, Double> variables) {
      final double[] values =
          arguments.stream().mapToDouble(argument -> argument.evaluate(variables)).toArray();
      return switch (name) {
        case "abs" -> Math.abs(values[0]);
        case "sqrt" -> Math.sqrt(values[0]);
        case "exp" -> Math.exp(values[0]);
        case "log", "ln" -> Math.log(values[0]);
        case "log10" -> Math.log10(values[0]);
        case "sin" -> Math.sin(values[0]);
        case "cos" -> Math.cos(values[0]);
        case "tan" -> Math.tan(values[0]);
        case "floor" -> Math.floor(values[0]);
        case "ceil" -> Math.ceil(values[0]);
        case "round" -> Math.rint(values[0]);
        case "min" -> Math.min(values[0], values[1]);
        case "max" -> Math.max(values[0], values[1]);
        case "pow" -> Math.pow(values[0], values[1]);
        default -> throw new IllegalStateException("Unknown formula function.");
      };
    }

    @Override
    public void variables(final Set<String> output) {
      arguments.forEach(argument -> argument.variables(output));
    }
  }

  private final String m_source;
  private final Expression m_expression;
  private final Set<String> m_variables;

  private FormulaExpression(final String source, final Expression expression) {
    m_source = source;
    m_expression = expression;
    final Set<String> variables = new LinkedHashSet<>();
    expression.variables(variables);
    m_variables = Set.copyOf(variables);
  }

  public static FormulaExpression compile(final String source) {
    if (source == null || source.isBlank())
      throw new IllegalArgumentException("Formula must not be empty.");
    final Parser parser = new Parser(source);
    final Expression expression = parser.expression();
    parser.skipWhitespace();
    if (!parser.finished()) parser.error("Unexpected character '" + parser.current() + "'");
    return new FormulaExpression(source, expression);
  }

  public Set<String> variables() {
    return m_variables;
  }

  public double evaluate(final Map<String, Double> variables) {
    final double value = m_expression.evaluate(variables);
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Formula produced a non-finite value: " + m_source);
    }
    return value;
  }

  // A small recursive-descent parser keeps formula evaluation deterministic and dependency-free.
  private static final class Parser {
    private final String m_source;
    private int m_position;

    Parser(final String source) {
      m_source = source;
    }

    Expression expression() {
      Expression result = term();
      while (true) {
        skipWhitespace();
        if (take('+')) result = new BinaryExpression('+', result, term());
        else if (take('-')) result = new BinaryExpression('-', result, term());
        else return result;
      }
    }

    Expression term() {
      Expression result = unary();
      while (true) {
        skipWhitespace();
        if (take('*')) result = new BinaryExpression('*', result, unary());
        else if (take('/')) result = new BinaryExpression('/', result, unary());
        else return result;
      }
    }

    Expression unary() {
      skipWhitespace();
      if (take('+')) return new UnaryExpression('+', unary());
      if (take('-')) return new UnaryExpression('-', unary());
      return power();
    }

    Expression power() {
      Expression result = primary();
      skipWhitespace();
      if (take('^')) result = new BinaryExpression('^', result, unary());
      return result;
    }

    Expression primary() {
      skipWhitespace();
      if (finished()) error("Expected a number, variable, function, or '('");
      if (take('(')) {
        final Expression result = expression();
        skipWhitespace();
        if (!take(')')) error("Expected ')'");
        return result;
      }
      if (current() == '[') return new VariableExpression(bracketedName('[', ']'));
      if (current() == '$') return new VariableExpression(bracketedName('$', '$'));
      if (Character.isDigit(current()) || current() == '.') return number();
      if (Character.isJavaIdentifierStart(current())) {
        final String name = identifier();
        skipWhitespace();
        if (!take('(')) {
          if ("pi".equalsIgnoreCase(name)) return new NumberExpression(Math.PI);
          if ("e".equalsIgnoreCase(name)) return new NumberExpression(Math.E);
          return new VariableExpression(name);
        }
        final List<Expression> arguments = new ArrayList<>();
        skipWhitespace();
        if (!take(')')) {
          do {
            arguments.add(expression());
            skipWhitespace();
          } while (take(','));
          if (!take(')')) error("Expected ')' after function arguments");
        }
        final String function = name.toLowerCase(Locale.ROOT);
        final int expected =
            switch (function) {
              case "abs",
                      "sqrt",
                      "exp",
                      "log",
                      "ln",
                      "log10",
                      "sin",
                      "cos",
                      "tan",
                      "floor",
                      "ceil",
                      "round" ->
                  1;
              case "min", "max", "pow" -> 2;
              default -> throw error("Unknown function '" + name + "'");
            };
        if (arguments.size() != expected) {
          error(name + " requires " + expected + " argument" + (expected == 1 ? "" : "s"));
        }
        return new FunctionExpression(function, List.copyOf(arguments));
      }
      throw error("Unexpected character '" + current() + "'");
    }

    Expression number() {
      final int start = m_position;
      boolean exponent = false;
      while (!finished()) {
        final char character = current();
        if (Character.isDigit(character) || character == '.') m_position++;
        else if ((character == 'e' || character == 'E') && !exponent) {
          exponent = true;
          m_position++;
          if (!finished() && (current() == '+' || current() == '-')) m_position++;
        } else break;
      }
      try {
        return new NumberExpression(Double.parseDouble(m_source.substring(start, m_position)));
      } catch (NumberFormatException exception) {
        throw error("Invalid number");
      }
    }

    String identifier() {
      final int start = m_position++;
      while (!finished() && Character.isJavaIdentifierPart(current())) m_position++;
      return m_source.substring(start, m_position);
    }

    String bracketedName(final char opening, final char closing) {
      if (!take(opening)) throw new IllegalStateException();
      final int start = m_position;
      while (!finished() && current() != closing) m_position++;
      if (finished()) error("Missing closing '" + closing + "' for variable name");
      final String result = m_source.substring(start, m_position).trim();
      m_position++;
      if (result.isEmpty()) error("Variable name must not be empty");
      return result;
    }

    boolean take(final char expected) {
      if (!finished() && current() == expected) {
        m_position++;
        return true;
      }
      return false;
    }

    void skipWhitespace() {
      while (!finished() && Character.isWhitespace(current())) m_position++;
    }

    boolean finished() {
      return m_position >= m_source.length();
    }

    char current() {
      return m_source.charAt(m_position);
    }

    IllegalArgumentException error(final String message) {
      throw new IllegalArgumentException(message + " at position " + (m_position + 1) + '.');
    }
  }
}
