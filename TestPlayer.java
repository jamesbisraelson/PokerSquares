import java.util.Collections;
import java.util.Stack;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

class Heuristic {
  private int count;
  private double score;

  public Heuristic(int score) {
    this.score = score;
  }

  public void addScore(int newScore) {
    double totalScore = this.count * this.score;
    totalScore += newScore;
    this.count++;
    this.score = totalScore / this.count;
  }

  public double getScore() {
    return this.score;
  }

  public String toString() {
    String output = "count: " + count + "\nscore: " + score + "\n";
    return output;
  }
}

public class TestPlayer implements PokerSquaresPlayer {
  private Vector<String> handsInGame = new Vector<String>();

  private final int SIZE = 5;
  private Card[][] grid = new Card[SIZE][SIZE];
  private int numPlays = 0;

  private Stack<Integer> plays = new Stack<Integer>();

  @Override
  public void setPointSystem(PokerSquaresPointSystem system, long millis) {

  }

  @Override
  public void init() {
    // clear grid
    handsInGame.removeAllElements();
    for (int row = 0; row < SIZE; row++)
      for (int col = 0; col < SIZE; col++)
        grid[row][col] = null;
    // reset numPlays
    numPlays = 0;

    plays.clear();
    for (int i = 0; i < 25; i++)
      plays.push(i);
    Collections.shuffle(plays);
  }

  @Override
  public int[] getPlay(Card card, long millisRemaining) {
    numPlays++;
    int play = plays.pop(); // get the next random position for play
    int row = play / 5;
    int col = play % 5;
    grid[row][col] = card;
    storeHands();
    int[] playPos = { row, col }; // decode it into row and column
    return playPos; // return it
  }

  @Override
  public String getName() {
    return "RandomPlayer";
  }

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

  private void storeHand(Card[] hand, int numPlays) {
    String encoding = WetDogPlayer.getHandEncoding(hand, numPlays);
    this.handsInGame.add(encoding);
  }

  public Vector<String> getHandsInGame() {
    return this.handsInGame;
  }

  public static void main(String[] args) {
    int maxScore = 0;
    HashMap<String, Heuristic> handEncodings = new HashMap<String, Heuristic>();
    TestPlayer train = new TestPlayer();
    PokerSquaresPointSystem system = PokerSquaresPointSystem.getBritishPointSystem();
    PokerSquares ps = new PokerSquares(train, system);
    if (args.length < 1) {
      System.out.println("not enough arguments");
      return;
    }

    double minutes = Double.parseDouble(args[0]);
    double seconds = minutes * 60;
    long millis = (long) seconds * 1000;
    while (millis > 0) {
      long startTime = System.currentTimeMillis();
      int score = ps.play();
      if (score > maxScore) {
        maxScore = score;
      }

      Vector<String> handsInGame = train.getHandsInGame();
      for (String handEncoding : handsInGame) {
        Heuristic handHeuristic = handEncodings.putIfAbsent(handEncoding, new Heuristic(score));
        if (handHeuristic != null) {
          handHeuristic.addScore(score);
          handEncodings.replace(handEncoding, handHeuristic);
        }
      }
      long endTime = System.currentTimeMillis();
      millis -= (endTime - startTime);
      System.out.printf("time left: %.1fs    encodings: %d             \r", (millis / 1000.0), handEncodings.size());
    }
    System.out.println();

    HashMap<String, Double> encodingScores = new HashMap<String, Double>();
    int i = 0;
    for (HashMap.Entry<String, Heuristic> entry : handEncodings.entrySet()) {
      i++;
      String encoding = entry.getKey();
      Double score = entry.getValue().getScore();
      encodingScores.put(encoding, score);
    }
    WetDogPlayer.saveEncoding(encodingScores, WetDogPlayer.FILENAME);
    System.out.println("encodings: " + i);
    System.out.println("max score: " + maxScore);
  }
}
