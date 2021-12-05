public class Test {
  public static void main(String[] args) {
    Card[] hand = new Card[5];
    hand[0] = Card.getCard("3H");
    hand[1] = Card.getCard("4H");
    hand[2] = Card.getCard("5H");
    hand[3] = Card.getCard("6H");
    hand[4] = Card.getCard("7H");
    for (int i = 0; i < hand.length; i++) {
      System.out.println(hand[i]);
    }

    String e = WetDogPlayer.getHandEncoding(hand, 0);
    System.out.println(e);
  }
}
