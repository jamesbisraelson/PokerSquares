import java.util.Random;

/**
 * PossiblePokerHand: This enum signifies the possible poker hands. Each
 * possible poker hand has a name and an id. This is used in determining the
 * heuristic function for the Monte Carlo search.
 *
 * @author James Israelson
 */
public enum PossiblePokerHand {
	HIGH_CARD(0, "high card"), ONE_PAIR(1, "one pair"), TWO_PAIR(2, "two pair"), THREE_OF_A_KIND(3, "three of a kind"),
	STRAIGHT(4, "straight"), FLUSH(5, "flush"), FULL_HOUSE(6, "full house"), FOUR_OF_A_KIND(7, "four of a kind"),
	STRAIGHT_FLUSH(8, "straight flush");

	public static final int NUM_HANDS = PossiblePokerHand.values().length;
	public int id;
	public String name;

	PossiblePokerHand(int id, String name) {
		this.id = id;
		this.name = name;
	}

	/**
	 * Given a Card array (with no more than 4 Cards in it), return an array of
	 * PossiblePokerHands. Some code has been copied from getPokerHand() in
	 * PokerHand.java
	 *
	 * @param hand - a Possible Poker hand represented as an array of Card objects
	 *             which may contain null values
	 * @return classification of the given Poker hand
	 */
	public static PossiblePokerHand[] getPossiblePokerHands(Card[] hand) {
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

		// check if straight
		if (highRank - lowRank > 0) {
			if (highRank - lowRank < PokerSquares.SIZE) {
				straightPossible = true;
			}
		}

		// check if ace high straight
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

		// full house check
		boolean fullHousePossible = false;
		switch (numCards) {
		case 5:
			// full house
			if (rankCountCounts[3] == 1 && rankCountCounts[2] == 1) {
				fullHousePossible = true;
			}
			break;
		case 4:
			// three of a kind and another card
			if ((rankCountCounts[3] == 1 && rankCountCounts[1] == 1)) {
				fullHousePossible = true;
			}
			// two pairs
			else if (rankCountCounts[2] == 2) {
				fullHousePossible = true;
			}
			break;
		case 3:
			// three of a kind
			if (rankCountCounts[3] == 1) {
				fullHousePossible = true;
			}
			// a pair and another card
			else if (rankCountCounts[2] == 1) {
				fullHousePossible = true;
			}
			break;
		default:
			// two, 1, or zero cards are always eligible for a full house
			fullHousePossible = true;
		}

		if (flushPossible) {
			if (straightPossible) {
				list[listCount] = PossiblePokerHand.STRAIGHT_FLUSH; // Straight Flush
				listCount++;
			}
		}
		if (numCards - maxOfAKind <= 1) {
			list[listCount] = PossiblePokerHand.FOUR_OF_A_KIND; // Four of a Kind
			listCount++;
		}
		if (fullHousePossible) {
			list[listCount] = PossiblePokerHand.FULL_HOUSE; // Full House
			listCount++;
		}

		if (straightPossible) {
			list[listCount] = PossiblePokerHand.STRAIGHT; // Straight
			listCount++;
		} // TODO: decide if i want to track less than a straight
		return list;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Enum#toString()
	 */
	public String toString() {
		return name;
	}
}
