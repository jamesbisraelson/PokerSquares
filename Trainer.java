import java.util.Collections;
import java.util.Stack;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

/**
 * Trainer: A PokerSquaresPlayer that simulates random games and stores every
 * hand encountered in the game into a Vector<String>.
 *
 * Some of the code in this file is adopted from the RandomPlayer that was
 * supplied as an example.
 *
 *
 * @author James Israelson
 * @author Todd W. Neller [getPlay(), init()]
 */
public class Trainer implements PokerSquaresPlayer {
  private Vector<String> handsInGame = new Vector<String>();
  private final int SIZE = 5;
  private Card[][] grid = new Card[SIZE][SIZE];
  private int numPlays = 0;

  private Stack<Integer> plays = new Stack<Integer>();

  @Override
  public void setPointSystem(PokerSquaresPointSystem system, long millis) {

  }

  /*
   * This method is taken from the RandomPlayer
   */
  @Override
  public void init() {
    handsInGame.removeAllElements();
    for (int row = 0; row < SIZE; row++)
      for (int col = 0; col < SIZE; col++)
        grid[row][col] = null;
    numPlays = 0;

    plays.clear();
    for (int i = 0; i < 25; i++)
      plays.push(i);
    Collections.shuffle(plays);
  }

  /*
   * This method is taken from the RandomPlayer with the exception of one minor
   * change.
   */
  @Override
  public int[] getPlay(Card card, long millisRemaining) {
    numPlays++;
    int play = plays.pop();
    int row = play / 5;
    int col = play % 5;
    grid[row][col] = card;

    /*
     * CHANGE: Store all of the hands from the grid into the handsInGame
     * Vector<String>.
     */
    storeHands();

    int[] playPos = { row, col };
    return playPos;
  }

  /*
   * Get the name for the Trainer
   *
   * @return The name for the player.
   */
  @Override
  public String getName() {
    return "Trainer";
  }

  /*
   * Store the each hand from the game grid into the handsInGame Vector<String>.
   */
  private void storeHands() {
    for (int row = 0; row < SIZE; row++) {
      Card[] hand = new Card[SIZE];
      for (int col = 0; col < SIZE; col++) {
        hand[col] = grid[row][col];
      }
      storeHand(hand, numPlays);
    }
    for (int col = 0; col < SIZE; col++) {
      Card[] hand = new Card[SIZE];
      for (int row = 0; row < SIZE; row++) {
        hand[row] = grid[row][col];
      }
      storeHand(hand, numPlays);
    }
  }

  /*
   * Get the hand encoding and store it in the handsInGame Vector<String>.
   *
   * @param hand A Card array representing a hand.
   *
   * @param numPlays The current number of plays in the game
   */
  private void storeHand(Card[] hand, int numPlays) {
    String encoding = JamesIsraelsonPlayer.getHandEncoding(hand, numPlays);
    this.handsInGame.add(encoding);
  }

  /*
   * Get the handsInGame
   *
   * @return The Vector<String> containing the hands in the game.
   */
  public Vector<String> getHandsInGame() {
    return this.handsInGame;
  }

  /*
   * Runs simulations of games repeatedly until a timer is up. Every hand in the
   * game is encoded into a String, and saved with its average score at the end of
   * the game into a file specified by JamesIsraelsonPlayer.FILENAME.
   *
   * @param args The first argument is used as the minutes to run the simulataions
   */
  public static void main(String[] args) {
    int maxScore = 0;

    // The HashMap in which to store the hand encodings with their Heuristic as the
    // value. The Heuristic contains the hand's average score and count of times
    // encountered.
    HashMap<String, Heuristic> handEncodings = new HashMap<String, Heuristic>();

    // Create the trainer and set up the game.
    Trainer train = new Trainer();
    PokerSquaresPointSystem system = PokerSquaresPointSystem.getBritishPointSystem();
    PokerSquares ps = new PokerSquares(train, system);

    // Get the number of minutes to run
    if (args.length < 1) {
      System.out.println("not enough arguments");
      return;
    }

    // Convert minutes to milliseconds
    double minutes = Double.parseDouble(args[0]);
    double seconds = minutes * 60;
    long millis = (long) seconds * 1000;

    // Run until millis == 0
    while (millis > 0) {
      // Get the current time
      long startTime = System.currentTimeMillis();
      System.out.printf("time left: %.1fs    encodings: %d             \r", (millis / 1000.0), handEncodings.size());

      // Play a game
      int score = ps.play();
      if (score > maxScore) {
        maxScore = score;
      }

      // Get the hands in the finished game
      Vector<String> handsInGame = train.getHandsInGame();

      // Convert each hand into a Heuristic and store it in handEncodings. If the
      // encoding already exists, update the Heuristic to account for the new score.
      for (String handEncoding : handsInGame) {
        Heuristic handHeuristic = handEncodings.putIfAbsent(handEncoding, new Heuristic(score));
        if (handHeuristic != null) {
          handHeuristic.addScore(score);
          handEncodings.replace(handEncoding, handHeuristic);
        }
      }

      // Get the current time
      long endTime = System.currentTimeMillis();

      // Decrement millis by the amount of time it took to run the iteration of the
      // while loop
      millis -= (endTime - startTime);
    }
    System.out.println();

    // Create a HashMap to hold the final encodings and their scores
    HashMap<String, Double> encodingScores = new HashMap<String, Double>();
    int i = 0;

    // Get each encoding's Heuristic score and add it to the HashMap<String, Double>
    for (HashMap.Entry<String, Heuristic> entry : handEncodings.entrySet()) {
      i++;
      String encoding = entry.getKey();
      Double score = entry.getValue().getScore();
      encodingScores.put(encoding, score);
    }

    // Save the encoding to a file specified by JamesIsraelsonPlayer.FILENAME
    JamesIsraelsonPlayer.saveEncoding(encodingScores, JamesIsraelsonPlayer.FILENAME);

    // Print the stats for the simulations
    System.out.println("encodings: " + i);
    System.out.println("max score: " + maxScore);
  }
}

/*
 * Heuristic: Represents the current score and count of a particular hand.
 *
 * @author James Israelson
 */
class Heuristic {
  // The amount of times the hand has been encountered.
  private int count;
  // The average score of the hand so far.
  private double score;

  /*
   * Creates a Heuristic.
   *
   * @param score The first score of the Heuristic.
   */
  public Heuristic(int score) {
    this.score = score;
  }

  /*
   * Adds a score to the Heuristic, increments the count, and averages out the
   * score.
   *
   * @param newScore The score to be added to the Heuristic
   */
  public void addScore(int newScore) {
    double totalScore = this.count * this.score;
    totalScore += newScore;
    this.count++;
    this.score = totalScore / this.count;
  }

  /*
   * Returns the current score for the Heuristic
   *
   * @return The score for the Heuristic
   */
  public double getScore() {
    return this.score;
  }

  /*
   * Returns the count and score for the Heuristic.
   *
   * @return The count and score for the Heuristic
   */
  public String toString() {
    String output = "count: " + count + "\nscore: " + score + "\n";
    return output;
  }
}
