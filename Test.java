public class Test {
  public static void main(String[] args) {
    Card[] hand = new Card[5];
    hand[0] = Card.getCard("7D");
    hand[1] = Card.getCard("QD");
    hand[2] = Card.getCard("6H");
    hand[3] = Card.getCard("7C");
    hand[4] = Card.getCard("TC");
    for (int i = 0; i < hand.length; i++) {
      System.out.println(hand[i]);
    }

    String e = WetDogPlayer.getHandEncoding(hand, 0);
    System.out.println(e);
  }
}
