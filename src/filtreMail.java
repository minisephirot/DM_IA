import java.io.*;
import java.util.*;

public class filtreMail {

    private static final boolean debug = false; // editer vrai si on veux voir des infos de debug

    public static void main(String args[]) {
        //Init
        if ( args.length != 2){
            System.out.println("Utilisation de filtreMail : 'nom du classificateur a utiliser' 'message a tester'");
            System.exit(1);
        }

        // FILTER UN SEUL MAIL
        System.out.println("Chargement du dictionnaire...");
        String[] dictionnaire = filtreMail.charger_dictionnaire();
        System.out.println("Dictionnaire chargé. "+dictionnaire.length+" mots ont été enregistrés.");
        if (debug) System.out.println("Liste des mots :"+Arrays.asList(dictionnaire)+"\n");

        HashMap<String,Double> vecteurx =  filtreMail.lire_message(dictionnaire,new File(args[1]));

        File fichier =  new File(args[0]) ;
        // ouverture d'un flux sur un fichier
        ObjectInputStream ois;
        Classifieur classifieur = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(fichier));
            // désérialization de l'objet
            classifieur = (Classifieur)ois.readObject() ;
            ois.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("Classifieur non trouvé");
            System.exit(1);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }

        double pXegalxSachantYegalSpam = getPdeXsachantYegalSpamOuHam(classifieur.probaSpam,vecteurx);
        double pXegalxSachantYegalHam = getPdeXsachantYegalSpamOuHam(classifieur.probaHam,vecteurx);
        double pDeXetYegalSpam = pXegalxSachantYegalSpam * classifieur.pYegalSpam;
        double pDeXetYegalHam = pXegalxSachantYegalHam * classifieur.pYegalHam;
        double pDeXegalx = pDeXetYegalHam + pDeXetYegalSpam;

        double pDeYegalSpamSachantXegalx = (1d/pDeXegalx) * classifieur.pYegalSpam * pXegalxSachantYegalSpam ;
        double pDeYegalHamSachantXegalx = (1d/pDeXegalx) * classifieur.pYegalHam * pXegalxSachantYegalHam;

        boolean isSpam = filtreMail.isSpam(pDeYegalSpamSachantXegalx,pDeYegalHamSachantXegalx);
        System.out.println("P(Y=SPAM | X=x) = "+pDeYegalSpamSachantXegalx+", P(Y=HAM | X=x) = "+pDeYegalHamSachantXegalx);
        System.out.print("D'apres ' "+ args[0]+"', le message '"+args[1]+"' est un ");
        if (isSpam){
            System.out.print("SPAM !\n");
        }else{
            System.out.print("HAM ! \n");
        }
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
