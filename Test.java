public class Test {
  public static void main(String[] args) {
    Card[] hand = new Card[5];
    hand[0] = Card.getCard("AH");
    // hand[1] = Card.getCard("AC");
    hand[2] = Card.getCard("JH");
    hand[3] = Card.getCard("JD");
    hand[4] = Card.getCard("AH");
    for (int i = 0; i < hand.length; i++) {
      System.out.println(hand[i]);
    }

    String e = WetDogPlayer.getHandEncoding(hand, 0);
    System.out.println(e);
  }
}
