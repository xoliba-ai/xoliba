package AI;

import Game.*;

import java.util.ArrayList;

/**
 * Created by vili on 13.6.2017.
 */
public class AlphaBetaXoliba {

    private Validator validator;
    private StoneCollector stoneCollector;
    private int inceptionTreshold;

    public AlphaBetaXoliba() {
        validator = new Validator();
        stoneCollector = new StoneCollector();
        inceptionTreshold = 3;
    }

    /**
     * Decide the best move by using alpha-beta pruning
     * @param board
     * @return all relevant info wrapped in a TurnData object
     */
    protected TurnData doTheBestMoveForRed(Board board, int inceptionTreshold) {
        this.inceptionTreshold = inceptionTreshold;
        TurnData td = new TurnData();
        int redBest = Integer.MIN_VALUE;
        int blueBest = Integer.MAX_VALUE;

        for (Move m: validator.generateAllPossibleMoves(board, 1)) { //for every move
            Board b1 = board.copy(); //we generate a board
            b1.swap(m); //where we implement the move
            for (Triangle t:m.triangles) { //and for every triangle with that board
                Board b2 = b1.copy(); //we make a board
                stoneCollector.hitStones(b2,t); //and implement the hitting
                int v = minValue(b2, 0, redBest, blueBest); //the opponent gets a move
                if (v > redBest) { //if the result is better than the known best
                    redBest = v; //update the best
                    td = new TurnData(true, b2.copy(), m, t, 1);
                    //System.out.println("AI updated it's planned move (redBest " + redBest + "; blueBest " + blueBest + ")\n" + td);
                }
            }
        }
        return td;
    }

    /**
     * Decide the best move by using alpha-beta pruning
     * @param board
     * @return all relevant info wrapped in a TurnData object
     */
    protected TurnData doTheBestMoveForBlue(Board board, int inceptionTreshold) {
        this.inceptionTreshold = inceptionTreshold;
        TurnData td = new TurnData();
        int redBest = Integer.MIN_VALUE;
        int blueBest = Integer.MAX_VALUE;

        for (Move m: validator.generateAllPossibleMoves(board, -1)) { //for every move
            Board b1 = board.copy(); //we generate a board
            b1.swap(m); //where we implement the move
            for (Triangle t:m.triangles) { //and for every triangle with that board
                Board b2 = b1.copy(); //we make a board
                stoneCollector.hitStones(b2,t); //and implement the hitting
                int v = maxValue(b2, 0, redBest, blueBest); //the opponent gets a move
                if (v < blueBest) { //if the result is better than the known best
                    blueBest = v; //update the best
                    td = new TurnData(true, b2, m, t, -1);
                }
            }
        }
        return td;
    }

    /**
     * "Reds turn"
     * estimate how good option this board would be for the red
     *
     * @param board
     * @param inceptionLevel
     * @param redsBest the biggest value predicted atm
     * @param bluesBest the smallest value predicted atm
     * @return the biggest possible (predicted) evaluation outcome from this game situation
     */
    private int maxValue(Board board, int inceptionLevel, int redsBest, int bluesBest) {
        //System.out.println("AI maxValue: inceptionLevel " + inceptionLevel + "; redsBest " + redsBest + "; bluesBest" + bluesBest);
        if (inceptionLevel > inceptionTreshold) {
            return board.evaluate();
        }

        ArrayList<Move> possibleMoves = validator.generateAllPossibleMoves(board, 1);
        if (possibleMoves.isEmpty()) {
            return minValue(board, inceptionLevel + 1, redsBest, bluesBest);
        }

        int v = Integer.MIN_VALUE;
        for (Move m: possibleMoves) { //for all possible moves for red
            Board b1 = board.copy();
            b1.swap(m);
            for (Triangle t: m.triangles) { //for all triangles we might form
                Board b = b1.copy(); //lets make a copy of this situation
                stoneCollector.hitStones(b, t); //lets do the move
                v = Math.max(v, minValue(b, inceptionLevel + 1, redsBest, bluesBest)); //lets keep the best possible outcome
                if (v >= bluesBest) { //this is the alpha-beta part: if our new value is better than the so-far-best (from our perspective)
                    return v;         //that opponent could choose, then we do have look no further
                }
                redsBest = Math.max(redsBest, v); //lets update the best value so far for future minValue invitations
            }
        }
        return v;
    }

    /**
     * "Blues turn"
     * estimate how good option this board would be for the blue
     *
     * @param board
     * @param inceptionLevel
     * @param redsBest the biggest value predicted atm
     * @param bluesBest the smallest value predicted atm
     * @return the smallest possible (predicted) evaluation outcome from this game situation
     */
    private int minValue(Board board, int inceptionLevel, int redsBest, int bluesBest) {
        if (inceptionLevel > inceptionTreshold) {
            return board.evaluate();
        }

        ArrayList<Move> possibleMoves = validator.generateAllPossibleMoves(board, -1);
        if (possibleMoves.isEmpty()) {
            return maxValue(board, inceptionLevel + 1, redsBest, bluesBest);
        }

        int v = Integer.MAX_VALUE;
        for (Move m: possibleMoves) { //for all possible moves for blue
            Board b1 = board.copy();
            b1.swap(m);
            for (Triangle t: m.triangles) { //for all triangles we might form
                Board b2 = b1.copy(); //lets make a copy of this situation
                stoneCollector.hitStones(b2, t); //lets do the move
                v = Math.min(v, maxValue(b2, inceptionLevel + 1, redsBest, bluesBest)); //lets keep the best possible outcome

                //this is the alpha-beta part: if our new value is better than the so-far-best (from our perspective)
                if (v <= redsBest) { //that opponent could choose, then we do have look no further
                    return v;
                }
                bluesBest = Math.min(bluesBest, v); //lets update the best value so far for future maxValue invitations
            }
        }
        return v;
    }
}