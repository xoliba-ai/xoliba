package Game;


/**
 * Created by vili on 6.6.2017.
 */
public class TurnData {

    public String type;     //for the JSON parser
    public boolean didMove;
    public int[][] board;   //7x7
    public int[] start;     //[x,y]
    public int[] target;    //[x,y]
    public int[][] corners; //[[x,y],[x,y]]

    public TurnData(boolean didMove, Board board, Move move, Triangle triangle) {
        this.type = "TurnData";
        this.didMove = didMove;
        this.board = board.board;
        this.start = new int[]{move.start.x, move.start.y};
        this.target = new int[]{move.target.x, move.target.y};

        corners = new int[2][2];
        int i = 0;
        for (Coordinate c:triangle.getCorners()) {
            if (c.equals(move.target)) { //we know that the target is part of the triangle
                continue;
            }
            corners[i] = new int[]{c.x, c.y};
            i++;
        }
    }

    /**
     * empty constructor for not doing a move
     */
    public TurnData() {
        type = "TurnData";
        didMove = false;
    }

    @Override
    public String toString() {
        String s = "" + type;
        s += didMove ? " is a move:\n" : " did not move";
        if (!didMove) return s;

        Coordinate c1 = new Coordinate(start[0], start[1]);
        Coordinate c2 = new Coordinate(target[0], target[1]);
        Board b = new Board(board);
        s += b;
        s += "triangles on board (both colors): " + b.getHowManyTrianglesOnBoard() + "\n";
        s += "start " + c1 + "; target " + c2 + ";\n";
        s += "triangle was formed at " + new Triangle(c2,
                new Coordinate(corners[0][0], corners[0][1]),
                new Coordinate(corners[1][0], corners[1][1]));
        s += "\n";
        return s;
    }
}