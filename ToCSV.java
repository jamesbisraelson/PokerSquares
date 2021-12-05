import java.util.HashMap;

public class ToCSV {
  public static void main(String[] args) {
    HashMap<String, Double> encodingScores = WetDogPlayer.loadEncoding(WetDogPlayer.FILENAME);
    for (HashMap.Entry<String, Double> entry : encodingScores.entrySet()) {
      String encoding = entry.getKey();
      Double score = entry.getValue();
      System.out.println(encoding + "," + score);
    }
  }
}
