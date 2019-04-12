import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class filtreAntiSpam {

    private static boolean debug = false;

    public static void main(String[] args) {

        //Init
        if (args.length != 3){
            System.out.println("Utilisation : 'dossier contenant les spams/ham' 'nb d'apprentissage sur spam' 'nb d'appentissage sur ham'");
        }
        args[0] = "basetest";
        args[1] = "100";
        args[2] = "200";

        Scanner sc = new Scanner(System.in);  // Create a Scanner object
        System.out.println("Debug ? Oui:true | Non :false");
        boolean debug = sc.nextBoolean();

        //Dictionnaire
        System.out.println("Chargement du dictionnaire...");
        String[] dictionnaire = filtreAntiSpam.charger_dictionnaire();
        System.out.println("Dictionnaire chargé. "+dictionnaire.length+" mots ont été enregistrés.\n");
        if (debug) System.out.println(Arrays.asList(dictionnaire));

        //Apprentissage
        System.out.println("Apprentissage...");
        int nbham = Objects.requireNonNull(new File("baseapp/ham").list()).length;
        int nbspam = Objects.requireNonNull(new File("baseapp/spam").list()).length;
        System.out.println("\t-Combien de SPAM de la base d’apprentissage ? Min = 1, Max = "+nbspam);
        nbspam = sc.nextInt();
        System.out.println("\t-Combien de HAM de la base d’apprentissage ? Min = 1, Max = "+nbham);
        nbham = sc.nextInt();
        //Création des b_spam et b_ham
        HashMap<String,Integer> probaSpam = new HashMap<>(dictionnaire.length); //On sait que l'on va utiliser uniquement les mots du dictionnaire.
        for (String mot : dictionnaire) {
            probaSpam.put(mot,1);//On a un lissage des parametres avec e = 1.
        }
        HashMap<String,Integer> probaHam = new HashMap<>(dictionnaire.length); //On sait que l'on va utiliser uniquement les mots du dictionnaire.
        for (String mot : dictionnaire) {
            probaHam.put(mot,0);//Le lissage ne s'applique pas pour ham.
        }
        if (debug) {
            System.out.println("Init des proba de spam :"+Collections.singletonList(probaSpam));
            System.out.println("Init des proba de ham :"+Collections.singletonList(probaHam));
            System.out.println("\nPour couper les mots et leurs ponctuations :");
            System.out.println("Ceci est un test, il n'est pas important");
            System.out.println(Arrays.asList("Ceci est un test, il n'est pas important".split("[\\s\\p{Punct}]+")));
        }
        //Apprentissage des SPAM:
        for (int i = 0; i < nbspam; i++) {
            HashMap<String,Integer> vecteurx =  filtreAntiSpam.lire_message(dictionnaire,new File("baseapp/spam/"+i+".txt"));
            filtreAntiSpam.mergeValues(probaSpam,vecteurx);
        }
        //Apprentissage des HAM:
        for (int i = 0; i < nbham; i++) {
            HashMap<String,Integer> vecteurx =  filtreAntiSpam.lire_message(dictionnaire,new File("baseapp/ham/"+i+".txt"));
            filtreAntiSpam.mergeValues(probaHam,vecteurx);
        }
        System.out.println("Après lecture des spam :"+Collections.singletonList(probaSpam));
        System.out.println("Après lecture des ham :"+Collections.singletonList(probaHam));

    }

    private static void mergeValues(HashMap<String, Integer> probaSpam, HashMap<String, Integer> vecteurx) {
        for(Map.Entry<String, Integer> entry : probaSpam.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();

            probaSpam.put(key,value + vecteurx.get(key));
        }
    }

    private static String[] charger_dictionnaire(){
        try {
            Scanner sc = new Scanner(new File("dictionnaire1000en.txt"));
            int i = 0; // Le dictionnaire a 1000 mots mais peux être amené a changer.
            while (sc.hasNextLine()) { // On compte le nombre de mots de taille > 3 chara.
                String s = sc.nextLine();
                if (s.length() > 2) i++;
            }
            String[] res = new String[i];

            sc = new Scanner(new File("dictionnaire1000en.txt"));
            int j = 0;
            while (sc.hasNextLine() && i >= j) {
                String s = sc.nextLine();
                if (s.length() > 2) {
                    res[j]= s;
                    j++;
                }
            }
            return res;

        } catch (FileNotFoundException e) {
            System.out.println("Erreur lors du chargement du dictionnaire.");
            System.exit(404);
        }
        return null;
    }

    private static HashMap<String,Integer> lire_message(String[] dictionnaire, File file){
        HashMap<String,Integer> res = new HashMap<>(dictionnaire.length); //On sait que l'on va utiliser uniquement les mots du dictionnaire.
        for (String mot : dictionnaire) {
            res.put(mot,0);//par défault le mot ne se trouve pas dans le message, on entre simplement les clés.
        }

        try {
            Scanner sc = new Scanner(file);
            while (sc.hasNextLine()) {
                String[] listeMots = sc.nextLine().split("[\\s\\p{Punct}]+"); // On lis le fichier ligne après ligne, et on coupe les lignes sur la ponctuation (source:https://stackoverflow.com/questions/35324047/reading-in-a-file-without-punctuation)
                for (String mot : listeMots) {                                      // Attention : on lis aussi les balises HMTL avec cette regex
                    boolean presence = res.containsKey(mot.toUpperCase());
                    if (presence){ // On a trouvé le mot du dictionnaire dans le message
                        res.put(mot.toUpperCase(),res.get(mot.toUpperCase())+1);
                    }
                }
            }

        } catch (FileNotFoundException e) {
            System.out.println("Erreur lors du chargement du fichier.("+file+")");
            System.exit(404);
        }

        return res;
    }
}
