public class Test {
  public static void main(String[] args) {
    Card[] hand = new Card[5];
    hand[0] = Card.getCard("TH");
    hand[1] = Card.getCard("AH");
    hand[2] = Card.getCard("JH");
    hand[3] = Card.getCard("QH");
    hand[4] = Card.getCard("KH");
    for (int i = 0; i < hand.length; i++) {
      System.out.println(hand[i]);
    }

    String e = WetDogPlayer.getHandEncoding(hand, 0);
    System.out.println(e);
  }
}
