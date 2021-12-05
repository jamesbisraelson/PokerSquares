import java.util.Collections;
import java.util.Stack;
import java.util.HashMap;
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
  private HashMap<Card[], Integer> handsInGame = new HashMap<Card[], Integer>();

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
    addHandsToHashMap();
    int[] playPos = { row, col }; // decode it into row and column
    return playPos; // return it
  }

  @Override
  public String getName() {
    return "RandomPlayer";
  }

  public void addHandsToHashMap() {
    for (int row = 0; row < SIZE; row++) {
      Card[] hand = new Card[SIZE];
      for (int col = 0; col < SIZE; col++) {
        hand[col] = grid[row][col];
      }
      handsInGame.put(hand, numPlays);
    }
    for (int col = 0; col < SIZE; col++) {
      Card[] hand = new Card[SIZE];
      for (int row = 0; row < SIZE; row++) {
        hand[row] = grid[row][col];
      }
      handsInGame.put(hand, numPlays);
    }
  }

  public HashMap<Card[], Integer> getHandsInGame() {
    return this.handsInGame;
  }

  public static void main(String[] args) {
    int maxScore = 0;
    HashMap<String, Heuristic> heuristicMap = new HashMap<String, Heuristic>();

    TestPlayer train = new TestPlayer();
    PokerSquaresPointSystem system = PokerSquaresPointSystem.getBritishPointSystem();
    for (int j = 0; j < 100; j++) {
      for (int i = 0; i < 1; i++) {
        int score = new PokerSquares(train, system).play();
        if (score > maxScore) {
          maxScore = score;
        }

        HashMap<Card[], Integer> handsInGame = train.getHandsInGame();

        int count = 0;
        for (HashMap.Entry<Card[], Integer> entry : handsInGame.entrySet()) {
          Card[] hand = entry.getKey();
          int numPlays = entry.getValue();

          String encoding = WetDogPlayer.getHandEncoding(hand, numPlays);
          System.out.println("encoding: " + encoding);
          System.out.println("move: " + numPlays);
          System.out.println("score: " + score + "\n");
          Heuristic h = heuristicMap.putIfAbsent(encoding, new Heuristic(score));
          if (h != null) {
            h.addScore(score);
            heuristicMap.replace(encoding, h);
          }
        }
      }

      int i = 0;
      HashMap<String, Double> heuristicScores = new HashMap<String, Double>();
      for (HashMap.Entry<String, Heuristic> entry : heuristicMap.entrySet()) {
        String encoding = entry.getKey();
        double score = entry.getValue().getScore();
        heuristicScores.put(encoding, score);
        i++;
      }
      WetDogPlayer.saveEncoding(heuristicScores, WetDogPlayer.FILENAME);
      System.out.println("encodings: " + i);
      System.out.println("max score: " + maxScore);
    }
  }
}
