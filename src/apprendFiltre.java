import java.io.*;
import java.util.*;

public class apprendFiltre {

    private static final boolean debug = false; // editer vrai si on veux voir des infos de debug

    public static void main(String args[]) {
        //Init
        if ( args.length != 4){
            System.out.println("Utilisation de apprendFiltre : 'nom du fichier sortie' 'dossier contenant la base d'apprentissage' 'nb de spam de la base d'apprentissage' 'nb de ham de la base d'apprentissage'");
            System.exit(1);
        }

        //Dictionnaire
        System.out.println("Chargement du dictionnaire...");
        String[] dictionnaire = apprendFiltre.charger_dictionnaire();
        System.out.println("Dictionnaire chargé. "+dictionnaire.length+" mots ont été enregistrés.");
        if (debug) System.out.println("Liste des mots :"+Arrays.asList(dictionnaire)+"\n");

        //Apprentissage
        double nbham = Objects.requireNonNull(new File(args[1]+"/ham").list()).length;
        double nbspam = Objects.requireNonNull(new File(args[1]+"/spam").list()).length;
        if(Integer.parseInt(args[2]) < nbspam && Integer.parseInt(args[3]) < nbham ){
            nbspam = Integer.parseInt(args[2]);
            nbham = Integer.parseInt(args[3]);
        }

        System.out.println("Apprentissage...");

        //Création des b_spam et b_ham
        HashMap<String,Double> probaSpam = new HashMap<>(dictionnaire.length); //On sait que l'on va utiliser uniquement les mots du dictionnaire.
        for (String mot : dictionnaire) {
            probaSpam.put(mot,1d);//On a un lissage des parametres avec e = 1.
        }
        HashMap<String,Double> probaHam = new HashMap<>(dictionnaire.length); //On sait que l'on va utiliser uniquement les mots du dictionnaire.
        for (String mot : dictionnaire) {
            probaHam.put(mot,0d);//Le lissage ne s'applique pas pour ham.
        }
        if (debug) {
            System.out.println("Init des proba de spam :"+Collections.singletonList(probaSpam));
            System.out.println("Init des proba de ham :"+Collections.singletonList(probaHam));
        }

        String apprentissage = args[1];
        //Apprentissage des SPAM:
        for (int i = 0; i < nbspam; i++) {
            HashMap<String,Double> vecteurx =  apprendFiltre.lire_message(dictionnaire,new File(apprentissage+"/spam/"+i+".txt"));
            apprendFiltre.mergeValues(probaSpam,vecteurx);
        }
        //Apprentissage des HAM:
        for (int i = 0; i < nbham; i++) {
            HashMap<String,Double> vecteurx =  apprendFiltre.lire_message(dictionnaire,new File(apprentissage+"/ham/"+i+".txt"));
            apprendFiltre.mergeValues(probaHam,vecteurx);
        }
        if (debug){
            System.out.println("Effectif des mots après lecture des spam :"+Collections.singletonList(probaSpam));
            System.out.println("Effectif des mots après lecture des ham :"+Collections.singletonList(probaHam));
        }
        //On a compté l'effectif d'apparition des mots dans les 2 catégories, on doit maintenant diviser ces effectifs par
        //Leurs nombre respectif de spam/ham avec +2 pour les spam car nous lissons ces probabilitées.
        apprendFiltre.effectifToFrequency(probaSpam,nbspam+2);
        apprendFiltre.effectifToFrequency(probaHam,nbham);
        if (debug){
            System.out.println("\nFrequence d'apparition des mots (spam) :"+Collections.singletonList(probaSpam));
            System.out.println("Frequence d'apparition des mots (ham) :"+Collections.singletonList(probaHam));
        }

        //On a besoin de p(Y=SPAM) et p(Y=HAM)
        double pYegalSpam = nbspam / (nbspam+nbham);
        double pYegalHam = 1d - pYegalSpam;
        if (debug) System.out.println("Probabilité qu'un message soit un spam vs Probabilité qu'un message soit un ham = "+ pYegalSpam+" contre "+ pYegalHam);

        System.out.println("Serialization...");
        Classifieur classifieur = new Classifieur(probaSpam, probaHam, pYegalSpam, pYegalHam);
        File fichier =  new File(args[0] + ".ser") ;

        // ouverture d'un flux sur un fichier
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(fichier));
            // sérialization de l'objet
            oos.writeObject(classifieur) ;
            oos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Creation de "+args[0] + ".ser terminée");
    }

    //Methode qui return a > b pour de très petit nombres
    private static boolean isSpam(double pDeYegalSpamSachantXegalx, double pDeYegalHamSachantXegalx) {
        double a = Math.log(pDeYegalSpamSachantXegalx);
        double b = Math.log(pDeYegalHamSachantXegalx);
        return a > b;
    }

    //Methode qui réalise la formule du diapo
    private static double getPdeXsachantYegalSpamOuHam(HashMap<String, Double> frequency, HashMap<String, Double> presence){
        double res = 1d;
        for(Map.Entry<String, Double> entry : frequency.entrySet()) {
            String key = entry.getKey();

            double motPresent = presence.get(key);
            if (motPresent >= 1d){
                res *= frequency.get(key);
            }else if (motPresent == 0d){
                res *= 1d-frequency.get(key);
            }else{
                System.out.println("Erreur : vecteur de presence invalide");
                System.exit(666);
            }
        }
        return res;
    }

    //Methode qui additionne message après message les effectifs de presence
    private static void mergeValues(HashMap<String, Double> effectif, HashMap<String, Double> vecteurx) {
        for(Map.Entry<String, Double> entry : effectif.entrySet()) {
            String key = entry.getKey();
            Double value = entry.getValue();
            effectif.put(key,value + vecteurx.get(key));
        }
    }

    //Methode qui transforme les effectifs en fréquence avec le total de ham/spam
    private static void effectifToFrequency(HashMap<String, Double> effectif, Double total) {
        for(Map.Entry<String, Double> entry : effectif.entrySet()) {
            String key = entry.getKey();
            Double value = entry.getValue()/total;
            effectif.put(key,value);
        }
    }

    //Methode qui charge le dictionnaire initial
    private static String[] charger_dictionnaire(){
        try {
            Scanner sc = new Scanner(new File("dictionnaire1000en.txt"));
            int i = 0; // Le dictionnaire a 1000 mots mais peux Ãªtre amené a changer.
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

    //Methode qui génère le vecteur de presence
    private static HashMap<String,Double> lire_message(String[] dictionnaire, File file){
        HashMap<String,Double> res = new HashMap<>(dictionnaire.length); //On sait que l'on va utiliser uniquement les mots du dictionnaire.
        for (String mot : dictionnaire) {
            res.put(mot,0d);//par défault le mot ne se trouve pas dans le message, on entre simplement les clés.
        }

        try {
            Scanner sc = new Scanner(file);
            while (sc.hasNextLine()) {
                String[] listeMots = sc.nextLine().split("[\\s\\p{Punct}]+"); // On lis le fichier ligne après ligne, et on coupe les lignes sur la ponctuation (source:https://stackoverflow.com/questions/35324047/reading-in-a-file-without-punctuation)
                for (String mot : listeMots) {                                      // Attention : on lis aussi les balises HMTL avec cette regex
                    boolean presence = res.containsKey(mot.toUpperCase()); //evite le casse
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
