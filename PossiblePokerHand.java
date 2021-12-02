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
	 * PossiblePokerHands.
	 *
	 * @param hand - a Poker hand represented as an array of Card objects which may
	 *             contain null values
	 * @return classification of the given Poker hand
	 */
	public static PossiblePokerHand[] getPossiblePokerHands(Card[] hand) {
		PossiblePokerHand[] list = new PossiblePokerHand[PossiblePokerHand.NUM_HANDS];
		list[0] = PossiblePokerHand.TWO_PAIR;
		list[0] = PossiblePokerHand.THREE_OF_A_KIND;
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
