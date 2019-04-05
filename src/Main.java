import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        System.out.println("Chargement du dictionnaire...");
        String[] res = Main.charger_dictionnaire();
    }

    private static String[] charger_dictionnaire(){
        try {
            Scanner sc = new Scanner(new File("res/dictionnaire1000en.txt"));
            int i = 0; // Le dictionnaire a 1000 mots mais peux être amené a changer.
            while (sc.hasNextLine()) { // On compte le nombre de mots de taille > 3 chara.
                String s = sc.nextLine();
                if (s.length() > 2) i++;
            }
            String[] res = new String[i];

            sc = new Scanner(new File("res/dictionnaire1000en.txt"));
            int j = 0;
            while (sc.hasNextLine() && i >= j) {
                String s = sc.nextLine();
                if (s.length() > 2) {
                    res[j] = s;
                    j++;
                }
            }
            System.out.println("Dictionnaire chargé. "+i+" mots ont été enregistrés.");
            return res;

        } catch (FileNotFoundException e) {
            System.out.println("Erreur lors du chargement du dictionnaire.");
            System.exit(404);
        }
        return null;
    }
}
