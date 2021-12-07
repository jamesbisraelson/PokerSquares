import java.util.Random;

/**
 * PossiblePokerHand: This enum signifies the possible poker hands for my
 * heuristic. Each possible poker hand has a name and an id. This is used in
 * determining the heuristic function for the Monte Carlo search.
 *
 * The enum is quite similar to PokerHand, but the methods are quite different.
 *
 *
 * @author James Israelson
 * @author Todd W. Neller (parts of enum)
 */
public enum PossiblePokerHand {
	ONE_PAIR(1, "one pair"), TWO_PAIR(2, "two pair"), THREE_OF_A_KIND(3, "three of a kind"), STRAIGHT(4, "straight"),
	FLUSH(5, "flush"), FULL_HOUSE(6, "full house"), FOUR_OF_A_KIND(7, "four of a kind"),
	STRAIGHT_FLUSH(8, "straight flush"), CARD_NO_PAIR_1(9, "1 card with no pair"),
	CARD_NO_PAIR_2(10, "2 cards with no pairs"), CARD_NO_PAIR_3(11, "3 cards with no pairs"),
	CARD_NO_PAIR_4(12, "4 cards with no pairs"), CARD_NO_PAIR_5(13, "5 cards with no pairs");

	public static final int NUM_HANDS = PossiblePokerHand.values().length;
	public int id;
	public String name;

	PossiblePokerHand(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public String toString() {
		return name;
	}

	/**
	 * Given a Card array (with no more than 4 Cards in it), return an array of
	 * PossiblePokerHands. Some code was inspired from getPokerHand() in PokerHand
	 *
	 * @param hand A Possible Poker hand represented as an array of Card objects
	 *             which may contain null values
	 * @return Classification of the given Poker hand
	 */
	public static PossiblePokerHand[] getPossiblePokerHands(Card[] hand) {
		// The first part of the method is inspired by getPokerHand()
		int numCards = 0;
		PossiblePokerHand[] list = new PossiblePokerHand[PossiblePokerHand.NUM_HANDS];
		int listCount = 0;

		int[] rankCounts = new int[Card.NUM_RANKS];
		int[] suitCounts = new int[Card.NUM_SUITS];

		// Compute counts
		for (Card card : hand) {
			if (card != null) {
				numCards++;
				rankCounts[card.getRank()]++;
				suitCounts[card.getSuit()]++;
			}
		}

		// Compute count of rank counts
		int maxOfAKind = 0;
		int[] rankCountCounts = new int[hand.length + 1];
		for (int count : rankCounts) {
			rankCountCounts[count]++;
			if (count > maxOfAKind) {
				maxOfAKind = count;
			}
		}

		// After this point, it is all my own code.

		// Flush check
		boolean flushPossible = false;
		if (numCards <= 1) {
			flushPossible = true;
		} else {
			for (int i = 0; i < Card.NUM_SUITS; i++) {
				if (suitCounts[i] != 0) {
					if (suitCounts[i] == numCards) {
						flushPossible = true;
					}
					break;
				}
			}
		}

		// Straight check
		boolean straightPossible = false;
		int lowRank = 0;
		while (lowRank <= Card.NUM_RANKS - numCards - 1 && rankCounts[lowRank] == 0) {
			lowRank++;
		}

		int highRank = Card.NUM_RANKS - 1;
		while (highRank >= lowRank && rankCounts[highRank] == 0) {
			highRank--;
		}

		if (highRank - lowRank > 0) {
			if (highRank - lowRank < PokerSquares.SIZE) {
				straightPossible = true;
			}
		}

		// Check if ace high straight
		if ((rankCounts[0] == 1)
				&& (rankCounts[12] == 1 || rankCounts[11] == 1 || rankCounts[10] == 1 || rankCounts[9] == 1)) {

			straightPossible = true;
			for (int i = 1; i < 9; i++) {
				if (rankCounts[i] != 0) {
					straightPossible = false;
					break;
				}
			}
		}

		for (int i = 0; i < Card.NUM_RANKS; i++) {
			if (rankCounts[i] > 1) {
				straightPossible = false;
			}
		}

		// Full house check
		boolean fullHousePossible = false;
		switch (numCards) {
		case 5:
			// Full house
			if (rankCountCounts[3] == 1 && rankCountCounts[2] == 1) {
				fullHousePossible = true;
			}
			break;
		case 4:
			// Three of a kind and another card
			if ((rankCountCounts[3] == 1 && rankCountCounts[1] == 1)) {
				fullHousePossible = true;
			}
			// Two pairs
			else if (rankCountCounts[2] == 2) {
				fullHousePossible = true;
			}
			break;
		case 3:
			// Three of a kind
			if (rankCountCounts[3] == 1) {
				fullHousePossible = true;
			}
			// A pair and another card
			else if (rankCountCounts[2] == 1) {
				fullHousePossible = true;
			}
			break;
		default:
			// 2, 1, or 0 cards are always eligible for a full house
			fullHousePossible = true;
		}

		// Add PossiblePokerHands to array.
		// Flush
		if (flushPossible) {
			list[listCount] = PossiblePokerHand.FLUSH;
			listCount++;
		}
		// Straight
		if (straightPossible) {
			list[listCount] = PossiblePokerHand.STRAIGHT;
			listCount++;
		}
		// Four of a kind
		if (numCards - maxOfAKind <= 1) {
			list[listCount] = PossiblePokerHand.FOUR_OF_A_KIND;
			listCount++;
		}
		// Full house
		if (fullHousePossible) {
			list[listCount] = PossiblePokerHand.FULL_HOUSE;
			listCount++;
		}
		// Three of a kind
		if (numCards - maxOfAKind <= 2) {
			list[listCount] = PossiblePokerHand.THREE_OF_A_KIND;
			listCount++;
		}

		// Add the number of cards with no pairs to the array
		switch (rankCountCounts[1]) {
		case 1:
			list[listCount] = PossiblePokerHand.CARD_NO_PAIR_1;
			listCount++;
			break;
		case 2:
			list[listCount] = PossiblePokerHand.CARD_NO_PAIR_2;
			listCount++;
			break;
		case 3:
			list[listCount] = PossiblePokerHand.CARD_NO_PAIR_3;
			listCount++;
			break;
		case 4:
			list[listCount] = PossiblePokerHand.CARD_NO_PAIR_4;
			listCount++;
			break;
		case 5:
			list[listCount] = PossiblePokerHand.CARD_NO_PAIR_5;
			listCount++;
			break;
		}

		return list;
	}
}
