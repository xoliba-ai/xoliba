package Optimization;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import AI.*;
import Game.Board;
import Game.RoundRecord;
import Game.TurnData;
import Messaging.JsonConverter;

public class MatchMaker {

    Logger logger = LogManager.getLogger(Optimization.MatchMaker.class);
    int whiteDifficulty = 1;
    int blackDifficulty = 1;
    ParametersAI whiteParameters; //the champ
    ParametersAI blackParameters; //the challenger
    ArrayList<int[][]> boards;
    //boolean print = false;
    boolean runSingleThread = false;
    boolean keepRecord = false;

    public MatchMaker(int whiteDifficulty, ParametersAI whiteParameters, int blackDifficulty, ParametersAI blackParameters, boolean print, boolean runSingleThread) {
        this(whiteDifficulty, whiteParameters, blackDifficulty, blackParameters);
        //this.print = print;   //We have logging system now. Del this plz?
        this.runSingleThread = runSingleThread;
    }

    public MatchMaker(int whiteDifficulty, ParametersAI whiteParameters, int blackDifficulty, ParametersAI blackParameters) {
        this.whiteDifficulty = whiteDifficulty;
        this.blackDifficulty = blackDifficulty;
        this.whiteParameters = whiteParameters;
        this.blackParameters = blackParameters;
        this.boards = readBoards();
    }

    public AIMatchResult calculate(int howManyBoards) {
        return calculate(howManyBoards, this.keepRecord);
    }

    /**
     *
     * @param howManyBoards
     * @return a single double that describes how well the challenger performed; the bigger the better (for the challenger)
     */
    public AIMatchResult calculate(int howManyBoards, boolean keepRecord) {
        this.keepRecord = keepRecord;
        //ArrayList<RoundResult> results = new ArrayList<>();
        RoundResult finalResult = new RoundResult();
		
		Semaphore finished = new Semaphore(howManyBoards < boards.size() ? 1 - howManyBoards : 1 - boards.size()); // :P
		Semaphore threadCount = new Semaphore(8);
		Semaphore mutex = new Semaphore(1);
		
        for(int i=0; i<howManyBoards && i<boards.size(); i++) {
			int j = i;		// yes, this is necessary

            if (runSingleThread) {
                RoundResult rr = calculateRoundForBothRoles(boards.get(i));
                rr.roundNo = j+1;
                //logger.debug(rr + "\n" + rr.endGameMessagesToString());
                finalResult.add(rr, keepRecord);
            } else {
                Thread matchThread = new Thread(() -> {
                    acquire(threadCount);

                    RoundResult rr = calculateRoundForBothRoles(boards.get(j));
                    rr.roundNo = j+1;

                    acquire(mutex);
                    //results.add(rr);
                    finalResult.add(rr, keepRecord);
                    //logger.debug(rr + "\n" + rr.endGameMessagesToString());
                    mutex.release();
                    threadCount.release();
                    finished.release();
                });

			    matchThread.start();
            }
        }

        if (!runSingleThread) {
		    acquire(finished);
        }

		return new AIMatchResult(finalResult, 2 * howManyBoards, whiteDifficulty, whiteParameters, blackDifficulty, blackParameters);
    }
	
	private void acquire(Semaphore semaphore) {
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			logger.trace("Thread interrupted");
		}
	}

    public RoundResult calculateRoundForBothRoles(int[][] board) {
        AI aiWhite, aiBlack;
        RoundResult rr = new RoundResult();

        aiWhite = new AI(1, whiteDifficulty, whiteParameters);
        aiBlack = new AI(-1, blackDifficulty, blackParameters);
        RoundRecord record = calculateRound(aiWhite, aiBlack, board);
        rr.addRoundRecord(record, true);
        int result = record.result;
        if (result > 0) {
            rr.whitePoints += result;
            rr.whiteWins++;
        } else if (result < 0) {
            rr.blackPoints -= result;
            rr.blackWins++;
        }
        aiWhite = new AI(-1, whiteDifficulty, whiteParameters);
        aiBlack = new AI(1, blackDifficulty, blackParameters);
        record = calculateRound(aiBlack, aiWhite, board);
        result = record.result;
        rr.addRoundRecord(record, false);
        if(result > 0) {
            rr.blackPoints += result;
            rr.blackWins++;
        } else if (result < 0) {
            rr.whitePoints -= result;
            rr.whiteWins++;
        }

        rr.updateStats();

        return rr;
    }

    public RoundRecord calculateRound(AI red, AI blue, int[][] board) {
        RoundRecord record = new RoundRecord(new Board(board), red, blue);
        if (Board.redStartsGame(board)) {
            playUntilRoundEnded(red, blue, board, record);
        } else {
            playUntilRoundEnded(blue, red, board, record);
        }
        record.calculatePoints();
        return record;
    }

    public RoundRecord playUntilRoundEnded(AI firstAI, AI secondAI, int[][] board, RoundRecord record) {
        int turnsWithoutMoving = 0;
        AI[] ai = new AI[]{firstAI, secondAI};
        //System.out.println("play until round ends, board in the beginning\n" + new Board(board));
        //Right now max moves is set to 100, which is 50 turns
        TurnData result = ai[0].move(board, ai[0].color, ai[0].getDifficulty(), 0);
        //System.out.println("board after one move\n" + new Board(result.board));
        record.addTurn(result);
        TurnData oldResult = result;
        for(int i=1; i<100; i++) {
            //System.out.println("top of the loop, old result:\n" + oldResult);
            AI acting = ai[i%2];
            result = acting.move(oldResult.board, acting.color, acting.getDifficulty(), oldResult.withoutHit);
            record.addTurn(result);
            //System.out.println("ai no. " + i%2 + " did a move (result):\n" + result);
            turnsWithoutMoving = updateGameEndingIndicators(result, turnsWithoutMoving);
            if (turnsWithoutMoving > 2 || result.withoutHit == 30) {
                record.addEndGameMessage(parseGameEndedMessage(i%2 == 0, acting.color, turnsWithoutMoving, result.withoutHit));
                return record;
            }
            oldResult = result;
        }
        logger.error("Game stopped: too many rounds.");
        return record;
    }

    private int updateGameEndingIndicators(TurnData latestTurnData, int turnsWithoutMoving) {
        if(!latestTurnData.didMove) {
            turnsWithoutMoving += 2;
        } else if(turnsWithoutMoving > 0) {
            //System.out.println("turns without stones hit is " + turnsWithouMoving + ", lets decrease it by one");
            turnsWithoutMoving--;
        }

        //Lets check every second turn if AIs are in a loop.
        //Btw changing this to 0 will give slightly different points.
        /*
        if(i%4 == 1) {
            //if the situation is repetitive AND it's not about other AI not able to do a move
            if (new Board(oldResult.board).hashCode() == new Board(result.board).hashCode() && turnsWithouMoving < 2) {
                //There have been same board layout in the past: so the result wont change.
                //So now we assume AI will do the same move with the same board every time.
                System.out.println("return board 'same board':\n" + result);
                return result.board;
            }
            oldResult = result;
        }*/
        return turnsWithoutMoving;
    }

    private String parseGameEndedMessage(boolean starter, int whosTurn, int turnsWithoutMoving, int movesWithoutHitting) {
        String s = whosTurn == 1 ? "reds" : "blues";
        String reason = turnsWithoutMoving > 2 ? "AI couldn't do a move" : "there were " + movesWithoutHitting + " turns without hitting";
        s = "ended on " + s + (starter ? " (starting AI)" : " (second AI)") + " turn, because " + reason;
        return s;
    }

    //from int[][][] to arraylist<int[][]> through json
    private void writeBoards(int[][][] boards) {
        ArrayList<String> bs = new ArrayList<>();
        for (int i = 0; i < boards.length; i++) {
            bs.add(JsonConverter.jsonifyTable(boards[i]));
        }

        try {
            Path file = Paths.get("./src/main/resources/boardsJSON.txt");
            Files.write(file, bs, Charset.forName("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<int[][]> readBoards() {
        ArrayList<int[][]> p = new ArrayList<>();
        try {
            InputStream in = MatchMaker.class.getResourceAsStream("/boardsJSON.txt");
            BufferedReader bufferedReader =  new BufferedReader(new InputStreamReader(in));

            String line = "";
            while((line = bufferedReader.readLine()) != null) {
                if (line.length() < 3) {
                    continue;
                }
                p.add(JsonConverter.parseTable(line));
            }

            bufferedReader.close();
        } catch(Exception ex) {
            logger.fatal("Couldnt load boards", ex);
        }
        return p;
    }
}