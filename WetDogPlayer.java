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
 * WetDogPlayer: A simple Monte Carlo approach to a PokerSquares player. The
 * player simulates random playouts after each potential move, and averages
 * those playouts to a score that is decided by a heuristic.
 *
 * Author: James Israelson
 */
public class WetDogPlayer implements PokerSquaresPlayer {
	public static final String FILENAME = "heuristic.wet";
	private HashMap<String, Double> heuristic;
	private final int SIZE = 5; // number of rows/columns in square grid
	private final int NUM_POS = SIZE * SIZE; // number of positions in square grid
	private final int NUM_CARDS = Card.NUM_CARDS; // number of cards in deck
	private Random random = new Random(); // pseudorandom number generator for Monte Carlo simulation
	private int[] plays = new int[NUM_POS]; // positions of plays so far (index 0 through numPlays - 1) recorded as
																					// integers using row-major indices.
	// row-major indices: play (r, c) is recorded as a single integer r * SIZE + c
	// (See http://en.wikipedia.org/wiki/Row-major_order)
	// From plays index [numPlays] onward, we maintain a list of yet unplayed
	// positions.
	private int numPlays = 0; // number of Cards played into the grid so far
	private PokerSquaresPointSystem system; // point system
	private int depthLimit = 2; // default depth limit for Random Monte Carlo (MC) play
	private Card[][] grid = new Card[SIZE][SIZE]; // grid with Card objects or null (for empty positions)
	private Card[] simDeck = Card.getAllCards(); // a list of all Cards. As we learn the index of cards in the play deck,
																								// we swap each dealt card to its correct index. Thus, from index
																								// numPlays
	// onward, we maintain a list of undealt cards for MC simulation.
	private int[][] legalPlayLists = new int[NUM_POS][NUM_POS]; // stores legal play lists indexed by numPlays (depth)
	// (This avoids constant allocation/deallocation of such lists during the
	// selections of MC simulations.)

	/**
	 * Create a Random Monte Carlo player that simulates random play to depth 2.
	 */
	public WetDogPlayer() {
	}

	/**
	 * Create a Random Monte Carlo player that simulates random play to a given
	 * depth limit.
	 *
	 * @param depthLimit depth limit for random simulated play
	 */
	public WetDogPlayer(int depthLimit) {
		this.depthLimit = depthLimit;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see PokerSquaresPlayer#init()
	 */
	@Override
	public void init() {
		this.heuristic = loadEncoding(FILENAME);
		// clear grid
		for (int row = 0; row < SIZE; row++)
			for (int col = 0; col < SIZE; col++)
				grid[row][col] = null;
		// reset numPlays
		numPlays = 0;
		// (re)initialize list of play positions (row-major ordering)
		for (int i = 0; i < NUM_POS; i++)
			plays[i] = i;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see PokerSquaresPlayer#getPlay(Card, long)
	 */
	@Override
	public int[] getPlay(Card card, long millisRemaining) {
		/*
		 * With this algorithm, the player chooses the legal play that has the highest
		 * expected score outcome. This outcome is estimated as follows: For each move,
		 * many simulated random plays to the set depthLimit are performed and the
		 * (sometimes partially-filled) grid is scored. For each play simulation, random
		 * undrawn cards are drawn in simulation and the player picks a play position
		 * randomly. After many such plays, the average score per simulated play is
		 * computed. The play with the highest average score is chosen (breaking ties
		 * randomly).
		 */

		// match simDeck to actual play event; in this way, all indices forward from the
		// card contain a list of
		// undealt Cards in some permutation.
		int cardIndex = numPlays;
		while (!card.equals(simDeck[cardIndex]))
			cardIndex++;
		simDeck[cardIndex] = simDeck[numPlays];
		simDeck[numPlays] = card;

		if (numPlays < 24) { // not the forced last play
			// compute average time per move evaluation
			int remainingPlays = NUM_POS - numPlays; // ignores triviality of last play to keep a conservative margin for game
																								// completion
			long millisPerPlay = millisRemaining / remainingPlays; // dividing time evenly with future getPlay() calls
			long millisPerMoveEval = millisPerPlay / remainingPlays; // dividing time evenly across moves now considered
			// copy the play positions (row-major indices) that are empty
			System.arraycopy(plays, numPlays, legalPlayLists[numPlays], 0, remainingPlays);
			double maxAverageScore = Double.NEGATIVE_INFINITY; // maximum average score found for moves so far
			ArrayList<Integer> bestPlays = new ArrayList<Integer>(); // all plays yielding the maximum average score
			for (int i = 0; i < remainingPlays; i++) { // for each legal play position
				int play = legalPlayLists[numPlays][i];
				long startTime = System.currentTimeMillis();
				long endTime = startTime + millisPerMoveEval; // compute when MC simulations should end
				makePlay(card, play / SIZE, play % SIZE); // play the card at the empty position
				int simCount = 0;
				double scoreTotal = 0.0;
				while (System.currentTimeMillis() < endTime) { // perform as many MC simulations as possible through the
																												// allotted time
					// Perform a Monte Carlo simulation of random play to the depth limit or game
					// end, whichever comes first.
					scoreTotal += simPlay(depthLimit); // accumulate MC simulation scores
					simCount++; // increment count of MC simulations
				}
				undoPlay(); // undo the play under evaluation
				// update (if necessary) the maximum average score and the list of best plays
				double averageScore = scoreTotal / simCount;
				if (averageScore >= maxAverageScore) {
					if (averageScore > maxAverageScore)
						bestPlays.clear();
					bestPlays.add(play);
					maxAverageScore = averageScore;
				}
			}
			int bestPlay = bestPlays.get(random.nextInt(bestPlays.size())); // choose a best play (breaking ties randomly)
			// update our list of plays, recording the chosen play in its sequential
			// position; all onward from numPlays are empty positions
			int bestPlayIndex = numPlays;
			while (plays[bestPlayIndex] != bestPlay)
				bestPlayIndex++;
			plays[bestPlayIndex] = plays[numPlays];
			plays[numPlays] = bestPlay;
		}

		int[] playPos = { plays[numPlays] / SIZE, plays[numPlays] % SIZE }; // decode it into row and column
		makePlay(card, playPos[0], playPos[1]); // make the chosen play (not undoing this time)
		return playPos; // return the chosen play
	}

	/**
	 * From the chosen play, perform simulated Card draws and random placement
	 * (depthLimit) iterations forward and return the resulting grid score.
	 *
	 * @param depthLimit - how many simulated random plays to perform
	 * @return resulting grid score after random MC simulation to given depthLimit
	 */
	private double simPlay(int depthLimit) {
		if (depthLimit == 0) { // with zero depth limit, return current score
			return system.getScore(grid);
		} else { // up to the non-zero depth limit or to game end, iteratively make the given
							// number of random plays
			double score = Double.MIN_VALUE;
			double maxScore = Double.MIN_VALUE;
			int depth = Math.min(depthLimit, NUM_POS - numPlays); // compute real depth limit, taking into account game end
			for (int d = 0; d < depth; d++) {
				// generate a random card draw
				int c = random.nextInt(NUM_CARDS - numPlays) + numPlays;
				Card card = simDeck[c];
				// choose a random play from the legal plays

				int remainingPlays = NUM_POS - numPlays;
				System.arraycopy(plays, numPlays, legalPlayLists[numPlays], 0, remainingPlays);
				int c2 = random.nextInt(remainingPlays);
				int play = legalPlayLists[numPlays][c2];
				makePlay(card, play / SIZE, play % SIZE);
			}
			score = getTotalHeuristicScore(grid);

			// Undo MC plays.
			for (int d = 0; d < depth; d++) {
				undoPlay();
			}

			return score;
		}
	}

	private double getTotalHeuristicScore(Card[][] grid) {
		double[] handHeuristicScores = getHeuristicScores(grid);
		double totalHeuristicScore = 0.0;
		for (int i = 0; i < handHeuristicScores.length; i++) {
			totalHeuristicScore += handHeuristicScores[i];
		}
		return totalHeuristicScore;
	}

	private double[] getHeuristicScores(Card[][] grid) {
		double[] handHeuristicScores = new double[2 * SIZE];
		for (int row = 0; row < SIZE; row++) {
			Card[] hand = new Card[SIZE];
			for (int col = 0; col < SIZE; col++)
				hand[col] = grid[row][col];
			handHeuristicScores[row] = getHandHeuristicScore(hand);
		}
		for (int col = 0; col < SIZE; col++) {
			Card[] hand = new Card[SIZE];
			for (int row = 0; row < SIZE; row++)
				hand[row] = grid[row][col];
			handHeuristicScores[SIZE + col] = getHandHeuristicScore(hand);
		}
		return handHeuristicScores;
	}

	private double getHandHeuristicScore(Card[] hand) {
		String encoding = getHandEncoding(hand, this.numPlays);
		double score = heuristic.getOrDefault(encoding, 0.0);
		return score;
	}

	public void makePlay(Card card, int row, int col) {
		// match simDeck to event
		int cardIndex = numPlays;
		while (!card.equals(simDeck[cardIndex]))
			cardIndex++;
		simDeck[cardIndex] = simDeck[numPlays];
		simDeck[numPlays] = card;

		// update plays to reflect chosen play in sequence
		grid[row][col] = card;
		int play = row * SIZE + col;
		int j = 0;
		while (plays[j] != play)
			j++;
		plays[j] = plays[numPlays];
		plays[numPlays] = play;

		// increment the number of plays taken
		numPlays++;
	}

	public void undoPlay() { // undo the previous play
		numPlays--;
		int play = plays[numPlays];
		grid[play / SIZE][play % SIZE] = null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see PokerSquaresPlayer#setPointSystem(PokerSquaresPointSystem, long)
	 */
	@Override
	public void setPointSystem(PokerSquaresPointSystem system, long millis) {
		this.system = system;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see PokerSquaresPlayer#getName()
	 */
	@Override
	public String getName() {
		return "WetDogPlayerDepth" + this.depthLimit;
	}

	/**
	 * Demonstrate RandomMCPlay with Ameritish point system.
	 *
	 * @param args (not used)
	 */
	public static void main(String[] args) {
		PokerSquaresPointSystem system = PokerSquaresPointSystem.getBritishPointSystem();
		System.out.println(system);
		new PokerSquares(new WetDogPlayer(3), system).play(); // play a single game
	}

	public static void saveEncoding(HashMap<String, Double> encoding, String path) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
			oos.writeObject(encoding);
			oos.flush();
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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

	public static String getHandEncoding(Card[] hand, int numPlays) {
		String encoding = numPlays + " ";
		PokerHand achievedHand = PokerHand.getPokerHand(hand);
		PossiblePokerHand[] possibleHands = PossiblePokerHand.getPossiblePokerHands(hand);

		for (PossiblePokerHand possibleHand : possibleHands) {
			if (possibleHand == PossiblePokerHand.STRAIGHT_FLUSH) {
				String s = "f";
				if (achievedHand == PokerHand.STRAIGHT_FLUSH || achievedHand == PokerHand.ROYAL_FLUSH) {
					s = s.toUpperCase();
				}
				encoding += s;
			}
			if (possibleHand == PossiblePokerHand.STRAIGHT) {
				String s = "s";
				if (achievedHand == PokerHand.STRAIGHT) {
					s = s.toUpperCase();
				}
				encoding += s;
			}
			if (possibleHand == PossiblePokerHand.FULL_HOUSE) {
				String s = "h";
				if (achievedHand == PokerHand.FULL_HOUSE) {
					s = s.toUpperCase();
				}
				encoding += s;
			}
			if (possibleHand == PossiblePokerHand.FOUR_OF_A_KIND) {
				String s = "k";
				if (achievedHand == PokerHand.FOUR_OF_A_KIND) {
					s = s.toUpperCase();
				}
				encoding += s;
			}
		}
		return encoding;
	}

}
