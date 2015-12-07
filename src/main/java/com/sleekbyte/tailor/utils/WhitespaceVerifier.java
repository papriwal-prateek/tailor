package com.sleekbyte.tailor.utils;

import com.sleekbyte.tailor.common.Location;
import com.sleekbyte.tailor.common.Messages;
import com.sleekbyte.tailor.common.Rules;
import com.sleekbyte.tailor.output.Printer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

/**
 *  Contains utility functions that are used by various whitespace listeners to verify whitespace.
 */
public final class WhitespaceVerifier {

    private Printer printer;
    private Rules rule;

    public WhitespaceVerifier(Printer printer, Rules rule) {
        this.printer = printer;
        this.rule = rule;
    }

    public boolean checkLeftSpaces(Token left, Token op, int numSpaces) {
        return op.getLine() == left.getLine()
            && op.getCharPositionInLine() - ListenerUtil.getLastCharPositionInLine(left) != numSpaces + 1;
    }

    public boolean checkRightSpaces(Token right, Token op, int numSpaces) {
        return right.getLine() == op.getLine()
            && right.getCharPositionInLine() - ListenerUtil.getLastCharPositionInLine(op) != numSpaces + 1;
    }

    public boolean checkIfInline(Token one, Token two) {
        return one.getLine() != two.getLine();
    }

    /**
     * Verifies if a certain punctuation token is left associated (no space on the left, one space on the right).
     *
     * @param left Token on the left of the punctuation token
     * @param right Token on the right of the punctuation token
     * @param punc Punctuation token
     * @param puncStr String version of the punctuation to be used in violation messages
     */
    public void verifyPunctuationLeftAssociation(Token left, Token right, Token punc, String puncStr) {
        Location puncLocation = ListenerUtil.getTokenLocation(punc);

        if (checkIfInline(left, punc) || checkLeftSpaces(left, punc, 0)) {
            printer.error(rule, puncStr + Messages.AT_COLUMN + puncLocation.column + " "
                + Messages.NO_SPACE_BEFORE, puncLocation);
        }

        if (checkRightSpaces(right, punc, 1)) {
            printer.error(rule, puncStr + Messages.AT_COLUMN + puncLocation.column + " "
                + Messages.SPACE_AFTER, puncLocation);
        }
    }

    /**
     * Verifies if a particular punctuation token is space delimited (single space on either side).
     *
     * @param left Token on the left of the punctuation token
     * @param right Token on the right of the punctuation token
     * @param punc Punctuation token
     * @param puncStr String version of the punctuation to be used in violation messages
     */
    public void verifyPunctuationIsSpaceDelimited(Token left, Token right, Token punc, String puncStr) {
        Location puncLocation = ListenerUtil.getTokenLocation(punc);
        if (checkLeftSpaces(left, punc, 1)) {
            printer.error(rule, puncStr + Messages.AT_COLUMN + puncLocation.column + " "
                + Messages.SPACE_BEFORE, puncLocation);
        }

        if (checkRightSpaces(right, punc, 1)) {
            printer.error(rule, puncStr + Messages.AT_COLUMN + puncLocation.column + " "
                + Messages.SPACE_AFTER, puncLocation);
        }
    }

    /**
     * Verifies if a parenthesized construct does not contain whitespace immediately after the opening parenthesis and
     * immediately before the closing parenthesis.
     *
     * @param ctx Context comprised of parentheses
     */
    public void verifyParenthesisContentWhitespace(ParserRuleContext ctx) {
        Location openingParenthesisLoc = ListenerUtil.getContextStartLocation(ctx);
        Location closingParenthesisLoc = ListenerUtil.getContextStopLocation(ctx);

        // Handles cases where the parentheses only contain whitespace
        // Example: if ( ) {}
        if (ctx.getChildCount() == 2) {
            if (openingParenthesisLoc.line == closingParenthesisLoc.line
                && closingParenthesisLoc.column != openingParenthesisLoc.column + 1) {
                printer.error(rule, Messages.EMPTY_PARENTHESES + Messages.ILLEGAL_WHITESPACE,
                    new Location(openingParenthesisLoc.line, openingParenthesisLoc.column + 1) );
            }
            return;
        }

        Token openingParenthesis = ParseTreeUtil.getStopTokenForNode(ctx.getChild(0));
        Token contentStart = ParseTreeUtil.getStartTokenForNode(ctx.getChild(1));
        Token contentEnd = ParseTreeUtil.getStopTokenForNode(ctx.getChild(1));
        Token closingParenthesis = ParseTreeUtil.getStopTokenForNode(ctx.getChild(2));

        if (checkLeftSpaces(openingParenthesis, contentStart, 0)) {
            printer.error(rule, Messages.PARENTHESES_CONTENT + Messages.LEADING_WHITESPACE,
                new Location(openingParenthesisLoc.line, openingParenthesisLoc.column + 1));
        }
        if (checkRightSpaces(closingParenthesis, contentEnd, 0)) {
            Location contentStopLocation = ListenerUtil.getTokenEndLocation(contentEnd);
            printer.error(rule, Messages.PARENTHESES_CONTENT + Messages.NOT_END_SPACE, contentStopLocation);
        }
    }

    /**
     * Verifies that parenthesized constructs do not have a whitespace before the opening parenthesis.
     *
     * @param ctx Context comprised of parentheses
     */
    public void verifyParenthesisSurroundingWhitespace(ParserRuleContext ctx) {
        Token left = ParseTreeUtil.getStopTokenForNode(ParseTreeUtil.getLeftNode(ctx));
        Token openingParenthesis = ParseTreeUtil.getStartTokenForNode(ctx.getChild(0));

        if (checkLeftSpaces(left, openingParenthesis, 0)) {
            Location illegalWhitespaceLocation =  ListenerUtil.getTokenEndLocation(left);
            printer.error(rule, Messages.NO_WHITESPACE_BEFORE_PARENTHESES, illegalWhitespaceLocation);
        }
    }
}
