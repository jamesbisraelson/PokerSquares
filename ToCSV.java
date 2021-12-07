import java.util.HashMap;

public class ToCSV {
  public static void main(String[] args) {
    HashMap<String, Double> encodingScores = JamesIsraelsonPlayer.loadEncoding(JamesIsraelsonPlayer.FILENAME);
    for (HashMap.Entry<String, Double> entry : encodingScores.entrySet()) {
      String encoding = entry.getKey();
      Double score = entry.getValue();
      System.out.println(encoding + "," + score);
    }
  }
}
