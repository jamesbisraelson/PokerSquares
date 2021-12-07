import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.lang.ClassNotFoundException;

/**
 * JamesIsraelsonPlayer: A simple Monte Carlo approach to a PokerSquares player.
 * The player simulates random playouts after each potential move down to a
 * depth of 3, and averages those playouts to a score that is decided by a
 * heuristic. The player then selects the play that gets the highest heuristic
 * score.
 *
 * The Monte Carlo simulation approach was adopted from the RandomMCPlayer that
 * was supplied, but all of the code for the heuristic and the heuristic's
 * training was made by me.
 *
 *
 * @author James Israelson
 * @author Todd W. Neller (Original)
 * @author Michael W. Fleming (Modifications)
 */
public class JamesIsraelsonPlayer implements PokerSquaresPlayer {
	// Unchaged from the RandomMCPlayer:
	private final int SIZE = 5;
	private final int NUM_POS = SIZE * SIZE;
	private final int NUM_CARDS = Card.NUM_CARDS;
	private Random random = new Random();
	private int[] plays = new int[NUM_POS];
	private int depthLimit = 3;
	private int numPlays = 0;
	private PokerSquaresPointSystem system;
	private Card[][] grid = new Card[SIZE][SIZE];
	private Card[] simDeck = Card.getAllCards();
	private int[][] legalPlayLists = new int[NUM_POS][NUM_POS];

	// Variables that I added:

	// The filename for the heuristic
	public static final String FILENAME = "heuristic.wet";
	// The hashmap in which the heuristic's values are stored
	private HashMap<String, Double> heuristic;

	/****************************************************************/
	// These methods are largely unchanged from the RandomMCPlayer.
	// I have noted in comments where I made modifications.
	/****************************************************************/

	public JamesIsraelsonPlayer() {
	}

	public JamesIsraelsonPlayer(int depthLimit) {
		this.depthLimit = depthLimit;
	}

	@Override
	public void init() {
		this.heuristic = loadEncoding(FILENAME);
		for (int row = 0; row < SIZE; row++)
			for (int col = 0; col < SIZE; col++)
				grid[row][col] = null;
		numPlays = 0;
		for (int i = 0; i < NUM_POS; i++)
			plays[i] = i;
	}

	@Override
	public int[] getPlay(Card card, long millisRemaining) {
		int cardIndex = numPlays;
		while (!card.equals(simDeck[cardIndex]))
			cardIndex++;
		simDeck[cardIndex] = simDeck[numPlays];
		simDeck[numPlays] = card;

		if (numPlays < 24) {
			int remainingPlays = NUM_POS - numPlays;
			long millisPerPlay = millisRemaining / remainingPlays;
			long millisPerMoveEval = millisPerPlay / remainingPlays;
			System.arraycopy(plays, numPlays, legalPlayLists[numPlays], 0, remainingPlays);
			double maxAverageScore = Double.NEGATIVE_INFINITY;
			ArrayList<Integer> bestPlays = new ArrayList<Integer>();
			for (int i = 0; i < remainingPlays; i++) {
				int play = legalPlayLists[numPlays][i];
				long startTime = System.currentTimeMillis();
				long endTime = startTime + millisPerMoveEval;
				makePlay(card, play / SIZE, play % SIZE);
				int simCount = 0;
				double scoreTotal = 0.0;
				while (System.currentTimeMillis() < endTime) {
					scoreTotal += simPlay(depthLimit);
					simCount++;
				}
				undoPlay();
				double averageScore = scoreTotal / simCount;
				if (averageScore >= maxAverageScore) {
					if (averageScore > maxAverageScore)
						bestPlays.clear();
					bestPlays.add(play);
					maxAverageScore = averageScore;
				}
			}
			int bestPlay = bestPlays.get(random.nextInt(bestPlays.size()));
			int bestPlayIndex = numPlays;
			while (plays[bestPlayIndex] != bestPlay)
				bestPlayIndex++;
			plays[bestPlayIndex] = plays[numPlays];
			plays[numPlays] = bestPlay;
		}

		int[] playPos = { plays[numPlays] / SIZE, plays[numPlays] % SIZE };
		makePlay(card, playPos[0], playPos[1]);
		return playPos;
	}

	/*
	 * This function has one minor change which is noted below. The rest is
	 * unchanged from RandomMCPlayer
	 */
	private double simPlay(int depthLimit) {
		if (depthLimit == 0) {
			return system.getScore(grid);
		} else {
			double score = Double.MIN_VALUE;
			int depth = Math.min(depthLimit, NUM_POS - numPlays);
			for (int d = 0; d < depth; d++) {
				int c = random.nextInt(NUM_CARDS - numPlays) + numPlays;
				Card card = simDeck[c];
				int remainingPlays = NUM_POS - numPlays;
				System.arraycopy(plays, numPlays, legalPlayLists[numPlays], 0, remainingPlays);
				int c2 = random.nextInt(remainingPlays);
				int play = legalPlayLists[numPlays][c2];
				makePlay(card, play / SIZE, play % SIZE);
			}
			/*
			 * CHANGE: The score is no longer calculated as the current score for the grid.
			 * Now, the method getTotalHeuristicScore() is called, and the score for the
			 * grid, determined by the heuristic, is returned.
			 */
			score = getTotalHeuristicScore(grid);

			for (int d = 0; d < depth; d++) {
				undoPlay();
			}
			return score;
		}
	}

	public void makePlay(Card card, int row, int col) {
		int cardIndex = numPlays;
		while (!card.equals(simDeck[cardIndex]))
			cardIndex++;
		simDeck[cardIndex] = simDeck[numPlays];
		simDeck[numPlays] = card;

		grid[row][col] = card;
		int play = row * SIZE + col;
		int j = 0;
		while (plays[j] != play)
			j++;
		plays[j] = plays[numPlays];
		plays[numPlays] = play;

		numPlays++;
	}

	public void undoPlay() {
		numPlays--;
		int play = plays[numPlays];
		grid[play / SIZE][play % SIZE] = null;
	}

	@Override
	public void setPointSystem(PokerSquaresPointSystem system, long millis) {
		this.system = system;
	}

	/****************************************************************/
	// From here on, the functions have been coded by me. I have made
	// notes where I have used or repurposed other code from the
	// project.
	/****************************************************************/

	/*
	 * Get the name for the JamesIsraelsonPlayer
	 *
	 * @return The name for the player.
	 */
	@Override
	public String getName() {
		return "JamesIsraelsonPlayer";
	}

	/*
	 * Play a game with the British Point System.
	 *
	 * @param args Unused.
	 */
	public static void main(String[] args) {
		PokerSquaresPointSystem system = PokerSquaresPointSystem.getBritishPointSystem();
		System.out.println(system);
		new PokerSquares(new JamesIsraelsonPlayer(), system).play();
	}

	/*
	 * Get the heuristic scores for each hand, iterate through the scores, and
	 * calculate a total heuristic score the for all of the hands in the grid.
	 *
	 * @param grid A 2D Card array that represents the game grid.
	 *
	 * @return The Total heuristic score for all of the hands in the grid.
	 */
	private double getTotalHeuristicScore(Card[][] grid) {
		double[] handHeuristicScores = getHeuristicScores(grid);
		double totalHeuristicScore = 0.0;
		for (int i = 0; i < handHeuristicScores.length; i++) {
			totalHeuristicScore += handHeuristicScores[i];
		}
		return totalHeuristicScore;
	}

	/*
	 * Takes a Card grid and returns a double array of heuristic scores for the
	 * hands in the grid.
	 *
	 * @param grid A 2D Card array that represents the game grid.
	 *
	 * @return A double array representing the heuristic scores for each hand in the
	 * grid.
	 */
	private double[] getHeuristicScores(Card[][] grid) {
		/*
		 * This method is inspired by GetHandScores in PokerSquaresPointSystem. Most of
		 * the code is the same except for the call to getHeuristicScore().
		 */
		double[] handHeuristicScores = new double[2 * SIZE];
		for (int row = 0; row < SIZE; row++) {
			Card[] hand = new Card[SIZE];
			for (int col = 0; col < SIZE; col++)
				hand[col] = grid[row][col];
			handHeuristicScores[row] = getHeuristicScore(hand);
		}
		for (int col = 0; col < SIZE; col++) {
			Card[] hand = new Card[SIZE];
			for (int row = 0; row < SIZE; row++)
				hand[row] = grid[row][col];
			handHeuristicScores[SIZE + col] = getHeuristicScore(hand);
		}
		return handHeuristicScores;
	}

	/*
	 * This method gets the encoding for the hand and checks to see if the encoding
	 * is in the HashMap. If it is, return its value. If not, return 0.0.
	 *
	 * @param hand An array of cards representing a hand.
	 *
	 * @return The score for the hand.
	 */
	private double getHeuristicScore(Card[] hand) {
		String encoding = getHandEncoding(hand, this.numPlays);
		double score = heuristic.getOrDefault(encoding, 0.0);
		return score;
	}

	/*
	 * This method takes a hand and the current number of plays and returns a String
	 * encoding for the hand
	 *
	 * @param hand An array of cards representing a hand.
	 *
	 * @param numPlays The current number of plays in the game.
	 *
	 * @return A String encoding for the hand.
	 */
	public static String getHandEncoding(Card[] hand, int numPlays) {
		// Get the current hand type for the hand.
		PokerHand achievedHand = PokerHand.getPokerHand(hand);

		// Get the PossiblePokerHands for the hand.
		PossiblePokerHand[] possibleHands = PossiblePokerHand.getPossiblePokerHands(hand);

		// Add the number of plays to the beginning of the encoding.
		String encoding = numPlays + ":";

		// If the hand has a pair, add a 'p'. If the hand has two pairs, add a 'P'.
		if (achievedHand == PokerHand.ONE_PAIR) {
			encoding += "p";
		} else if (achievedHand == PokerHand.TWO_PAIR) {
			encoding += "P";
		}

		// Iterate through the PossiblePokerHands for the hand.
		for (PossiblePokerHand possibleHand : possibleHands) {

			// If a flush is possible, add a 'f'. If a flush has been acheived, add a 'F'.
			if (possibleHand == PossiblePokerHand.FLUSH) {
				String s = "f";
				if (achievedHand == PokerHand.FLUSH || achievedHand == PokerHand.ROYAL_FLUSH
						|| achievedHand == PokerHand.STRAIGHT_FLUSH) {
					s = s.toUpperCase();
				}
				encoding += s;
			}

			// If a straight is possible, add an 's'. If a straight has been acheived, add
			// an 'S'.
			if (possibleHand == PossiblePokerHand.STRAIGHT) {
				String s = "s";
				if (achievedHand == PokerHand.STRAIGHT || achievedHand == PokerHand.ROYAL_FLUSH
						|| achievedHand == PokerHand.STRAIGHT_FLUSH) {
					s = s.toUpperCase();
				}
				encoding += s;
			}

			// If a full house is possible, add an 'h'. If a full house has been acheived,
			// add an 'H'.
			if (possibleHand == PossiblePokerHand.FULL_HOUSE) {
				String s = "h";
				if (achievedHand == PokerHand.FULL_HOUSE) {
					s = s.toUpperCase();
				}
				encoding += s;
			}

			// If a four of a kind is possible, add an 'o'. If a four of a kind has been
			// acheived, add an 'O'.
			if (possibleHand == PossiblePokerHand.FOUR_OF_A_KIND) {
				String s = "o";
				if (achievedHand == PokerHand.FOUR_OF_A_KIND) {
					s = s.toUpperCase();
				}
				encoding += s;
			}

			// Add the number of cards with no pairs to the end of the encoding.
			String s = "";
			if (possibleHand == PossiblePokerHand.CARD_NO_PAIR_1) {
				s = "(1)";
			} else if (possibleHand == PossiblePokerHand.CARD_NO_PAIR_2) {
				s = "(2)";
			} else if (possibleHand == PossiblePokerHand.CARD_NO_PAIR_3) {
				s = "(3)";
			} else if (possibleHand == PossiblePokerHand.CARD_NO_PAIR_4) {
				s = "(4)";
			} else if (possibleHand == PossiblePokerHand.CARD_NO_PAIR_5) {
				s = "(5)";
			}
			encoding += s;
		}
		return encoding;
	}

	/*
	 * Takes a HashMap<String, Double> with encodings for hands and their scores and
	 * writes it to a file.
	 *
	 * @param encodings The HashMap<String, Double> that stores the encodings and
	 * scores for the hands. The key is the enccoding and the value is the score.
	 *
	 * @param path The path of the file in which the HashMap is saved.
	 */
	public static void saveEncoding(HashMap<String, Double> encodings, String path) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
			oos.writeObject(encodings);
			oos.flush();
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Takes a path to a file and loads a HashMap<String, Double> from that file.
	 *
	 * @param path The path of the file from which the HashMap is loaded.
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<String, Double> loadEncoding(String path) {
		HashMap<String, Double> encoding = null;
		try {
			ObjectInputStream ios = new ObjectInputStream(new FileInputStream(path));
			encoding = (HashMap<String, Double>) ios.readObject();
			ios.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return encoding;
	}

}
