package de.thecoolcraft11.commandBundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MathEvaluator {

    public static String evaluate(String expression) {
        try {
            expression = expression.trim();


            if (expression.contains("..")) {
                return evaluateRange(expression);
            }


            expression = replaceFunctions(expression);


            double result = evaluateExpression(expression);


            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return String.valueOf((long) result);
            }

            return String.valueOf(result);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private static String evaluateRange(String expression) {
        String[] parts = expression.split("\\.\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid range expression");
        }

        int start = (int) evaluateExpression(parts[0].trim());
        int end = (int) evaluateExpression(parts[1].trim());

        List<String> numbers = new ArrayList<>();
        if (start <= end) {
            for (int i = start; i <= end; i++) {
                numbers.add(String.valueOf(i));
            }
        } else {
            for (int i = start; i >= end; i--) {
                numbers.add(String.valueOf(i));
            }
        }

        return String.join(",", numbers);
    }

    private static String replaceFunctions(String expression) {

        Pattern pattern = Pattern.compile("(sqrt|int|round)\\s*\\(([^()]+)\\)");

        while (true) {
            Matcher matcher = pattern.matcher(expression);
            if (!matcher.find()) {
                break;
            }

            String function = matcher.group(1);
            String args = matcher.group(2);

            double argValue = evaluateExpression(args);
            double result = switch (function) {
                case "sqrt" -> Math.sqrt(argValue);
                case "int" -> (long) argValue;
                case "round" -> Math.round(argValue);
                default -> throw new IllegalArgumentException("Unknown function: " + function);
            };

            expression = expression.substring(0, matcher.start())
                    + result
                    + expression.substring(matcher.end());
        }

        return expression;
    }

    private static double evaluateExpression(String expression) {
        expression = expression.trim().replaceAll("\\s+", "");

        if (expression.isEmpty()) {
            throw new IllegalArgumentException("Empty expression");
        }

        List<String> postfix = infixToPostfix(expression);
        return evaluatePostfix(postfix);
    }

    private static List<String> infixToPostfix(String expression) {
        List<String> output = new ArrayList<>();
        Stack<String> operators = new Stack<>();

        int i = 0;
        while (i < expression.length()) {
            char c = expression.charAt(i);


            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }


            if (Character.isDigit(c) || c == '.') {
                StringBuilder number = new StringBuilder();
                while (i < expression.length() && (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    number.append(expression.charAt(i));
                    i++;
                }
                output.add(number.toString());
                continue;
            }


            if (c == '-' && (i == 0 || isOperator(expression.charAt(i - 1)) || expression.charAt(i - 1) == '(')) {
                i++;
                StringBuilder number = new StringBuilder("-");
                while (i < expression.length() && (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    number.append(expression.charAt(i));
                    i++;
                }
                output.add(number.toString());
                continue;
            }


            if (c == '(') {
                operators.push(String.valueOf(c));
                i++;
                continue;
            }


            if (c == ')') {
                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    output.add(operators.pop());
                }
                if (!operators.isEmpty()) {
                    operators.pop();
                }
                i++;
                continue;
            }


            if (isOperator(c)) {
                String op = String.valueOf(c);
                while (!operators.isEmpty() && !operators.peek().equals("(") &&
                        precedence(operators.peek()) >= precedence(op)) {
                    output.add(operators.pop());
                }
                operators.push(op);
                i++;
                continue;
            }

            i++;
        }


        while (!operators.isEmpty()) {
            output.add(operators.pop());
        }

        return output;
    }

    /**
     * Evaluate postfix expression
     */
    private static double evaluatePostfix(List<String> postfix) {
        Stack<Double> stack = new Stack<>();

        for (String token : postfix) {
            if (isOperatorString(token)) {
                if (stack.size() < 2) {
                    throw new IllegalArgumentException("Invalid expression");
                }

                double b = stack.pop();
                double a = stack.pop();
                double result = switch (token) {
                    case "+" -> a + b;
                    case "-" -> a - b;
                    case "*" -> a * b;
                    case "/" -> {
                        if (b == 0) {
                            throw new IllegalArgumentException("Division by zero");
                        }
                        yield a / b;
                    }
                    case "^" -> Math.pow(a, b);
                    case "%" -> a % b;
                    default -> throw new IllegalArgumentException("Unknown operator: " + token);
                };

                stack.push(result);
            } else {

                try {
                    stack.push(Double.parseDouble(token));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid number: " + token);
                }
            }
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("Invalid expression");
        }

        return stack.pop();
    }

    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^' || c == '%';
    }

    private static boolean isOperatorString(String s) {
        return s.length() == 1 && isOperator(s.charAt(0));
    }

    private static int precedence(String op) {
        return switch (op) {
            case "+", "-" -> 1;
            case "*", "/", "%" -> 2;
            case "^" -> 3;
            default -> 0;
        };
    }
}

