import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class filtreAntiSpam {

    private static final boolean debug = false; // editer vrai si on veux voir des infos de debug

    public static void main(String args[]) {
        //Init
        if ( args.length != 3){
            System.out.println("Utilisation de filtreAntiSpam : 'dossier contenant les spams/ham' 'nb spam test�' 'nb ham test�'");
            System.exit(1);
        }
        Scanner sc = new Scanner(System.in);  // Create a Scanner object

        //Dictionnaire
        System.out.println("Chargement du dictionnaire...");
        String[] dictionnaire = filtreAntiSpam.charger_dictionnaire();
        System.out.println("Dictionnaire charg�. "+dictionnaire.length+" mots ont �t� enregistr�s.");
        if (debug) System.out.println("Liste des mots :"+Arrays.asList(dictionnaire)+"\n");

        //Apprentissage
        double nbham = Objects.requireNonNull(new File("baseapp/ham").list()).length;
        double nbspam = Objects.requireNonNull(new File("baseapp/spam").list()).length;
        System.out.println("-Combien de SPAM de la base d'apprentissage ? Min = 1, Max = "+nbspam);
        nbspam = sc.nextInt();
        System.out.println("-Combien de HAM de la base d'apprentissage ? Min = 1, Max = "+nbham);
        nbham = sc.nextInt();


        System.out.println("Apprentissage...");
        //Cr�ation des b_spam et b_ham
        HashMap<String,Double> probaSpam = new HashMap<>(dictionnaire.length); //On sait que l'on va utiliser uniquement les mots du dictionnaire.
        for (String mot : dictionnaire) {
            probaSpam.put(mot,1d);//On a un lissage des parametres avec e = 1.
        }
        HashMap<String,Double> probaHam = new HashMap<>(dictionnaire.length); //On sait que l'on va utiliser uniquement les mots du dictionnaire.
        for (String mot : dictionnaire) {
            probaHam.put(mot,1d);//Le lissage ne s'applique pas pour ham.
        }
        if (debug) {
            System.out.println("Init des proba de spam :"+Collections.singletonList(probaSpam));
            System.out.println("Init des proba de ham :"+Collections.singletonList(probaHam));
            System.out.println("\nPour couper les mots et leurs ponctuations :");
            System.out.println("Ceci est un test, il n'est pas important");
            System.out.println(Arrays.asList("Ceci est un test, il n'est pas important".split("[\\s\\p{Punct}]+"))+"\n");
        }

        String apprentissage = "baseapp";
        //Apprentissage des SPAM:
        for (int i = 0; i < nbspam; i++) {
            HashMap<String,Double> vecteurx =  filtreAntiSpam.lire_message(dictionnaire,new File(apprentissage+"/spam/"+i+".txt"));
            filtreAntiSpam.mergeValues(probaSpam,vecteurx);
        }
        //Apprentissage des HAM:
        for (int i = 0; i < nbham; i++) {
            HashMap<String,Double> vecteurx =  filtreAntiSpam.lire_message(dictionnaire,new File(apprentissage+"/ham/"+i+".txt"));
            filtreAntiSpam.mergeValues(probaHam,vecteurx);
        }
        if (debug){
            System.out.println("Effectif des mots apr�s lecture des spam :"+Collections.singletonList(probaSpam));
            System.out.println("Effectif des mots apr�s lecture des ham :"+Collections.singletonList(probaHam));
        }
        //On a compt� l'effectif d'apparition des mots dans les 2 cat�gories, on doit maintenant diviser ces effectifs par
        //Leurs nombre respectif de spam/ham avec +2 pour les spam car nous lissons ces probabilit�es.
        filtreAntiSpam.effectifToFrequency(probaSpam,nbspam+2);
        filtreAntiSpam.effectifToFrequency(probaHam,nbham+2);
        if (debug){
            System.out.println("\nFrequence d'apparition des mots (spam) :"+Collections.singletonList(probaSpam));
            System.out.println("Frequence d'apparition des mots (ham) :"+Collections.singletonList(probaHam));
        }

        //On a besoin de p(Y=SPAM) et p(Y=HAM)
        double pYegalSpam = nbspam / (nbspam+nbham);
        double pYegalHam = 1d - pYegalSpam;
        if (debug) System.out.println("Probabilit� qu'un message soit un spam vs Probabilit� qu'un message soit un ham = "+ pYegalSpam+" contre "+ pYegalHam);

        //Tests
        double nbtestspam = Integer.parseInt(args[1]);
        double nbtestham = Integer.parseInt(args[2]);
        System.out.println("\nTests...");
        // p(X = x) que l'on obtiens via les probabilit�es totales :
        // p(X = x) = P(X = x , Y = SPAM) + P(X = x , Y = HAM)
        // Et P(X = x , Y = SPAM) = P(X = x | Y = SPAM) * P(Y = SPAM)
        // Et P(X = x , Y = HAM) = P(X = x | Y = HAM) * P(Y = HAM)
        // Enfin, P(X = x | Y = SPAM ou HAM) = les formule qui sont disponibles le diapo sur arche, diapo 52
        double nberreur = 0d;
        for (int i = 0; i < nbtestspam ; i++) {
            HashMap<String,Double> vecteurx =  filtreAntiSpam.lire_message(dictionnaire,new File(args[0]+"/spam/"+i+".txt"));
            double pXegalxSachantYegalSpam = getPdeXsachantYegalSpamOuHam(probaSpam,vecteurx);
            double pXegalxSachantYegalHam = getPdeXsachantYegalSpamOuHam(probaHam,vecteurx);
            double pDeXetYegalSpam = pXegalxSachantYegalSpam * pYegalSpam;
            double pDeXetYegalHam = pXegalxSachantYegalHam * pYegalHam;
            double pDeXegalx = pDeXetYegalHam + pDeXetYegalSpam;

            double pDeYegalSpamSachantXegalx = (1d/pDeXegalx) * pYegalSpam * pXegalxSachantYegalSpam ;
            double pDeYegalHamSachantXegalx = (1d/pDeXegalx) * pYegalHam * pXegalxSachantYegalHam;

            boolean isSpam = filtreAntiSpam.isSpam(pDeYegalSpamSachantXegalx,pDeYegalHamSachantXegalx);
            System.out.print("SPAM "+i+" : P(Y=SPAM | X=x) = "+pDeYegalSpamSachantXegalx+", P(Y=HAM | X=x) = "+pDeYegalHamSachantXegalx+" => identifi� comme un ");
            if (isSpam){
                System.out.print("SPAM !\n");
            }else{
                System.out.print("HAM ! *Erreur*\n");
                nberreur++;
            }
        }
        double erreurSpam = (nberreur/nbtestspam)*100d*10/10;

        nberreur = 0d;
        for (int i = 0; i < nbtestham ; i++) {
            HashMap<String,Double> vecteurx =  filtreAntiSpam.lire_message(dictionnaire,new File(args[0]+"/ham/"+i+".txt"));
            double pXegalxSachantYegalSpam = getPdeXsachantYegalSpamOuHam(probaSpam,vecteurx);
            double pXegalxSachantYegalHam = getPdeXsachantYegalSpamOuHam(probaHam,vecteurx);
            double pDeXetYegalSpam = pXegalxSachantYegalSpam * pYegalSpam;
            double pDeXetYegalHam = pXegalxSachantYegalHam * pYegalHam;
            double pDeXegalx = pDeXetYegalHam + pDeXetYegalSpam;

            double pDeYegalSpamSachantXegalx = (1d/pDeXegalx) * pYegalSpam * pXegalxSachantYegalSpam ;
            double pDeYegalHamSachantXegalx = (1d/pDeXegalx) * pYegalHam * pXegalxSachantYegalHam;

            boolean isSpam = filtreAntiSpam.isSpam(pDeYegalSpamSachantXegalx,pDeYegalHamSachantXegalx);
            System.out.print("HAM "+i+" : P(Y=SPAM | X=x) = "+pDeYegalSpamSachantXegalx+", P(Y=HAM | X=x) = "+pDeYegalHamSachantXegalx+" => identifi� comme un ");
            if (isSpam){
                System.out.print("SPAM ! *Erreur*\n");
                nberreur++;
            }else{
                System.out.print("HAM !\n");
            }
        }

        double erreurHam = (nberreur/nbtestham)*100d*10/10;
        double nbtotaltest = nbtestham + nbtestspam;
        double erreurTotale = (erreurHam * (nbtestham/nbtotaltest)) + (erreurSpam * (nbtestspam/nbtotaltest));
        System.out.println("Erreur de test sur les "+nbtestspam+" SPAM : "+erreurSpam+" %");
        System.out.println("Erreur de test sur les "+nbtestham+" HAM : "+erreurHam+" %");
        System.out.println("Erreur totale sur les "+nbtotaltest+" mails : "+erreurTotale+" %");
    }

    //Methode qui return a > b pour de tr�s petit nombres
    static boolean isSpam(double pDeYegalSpamSachantXegalx, double pDeYegalHamSachantXegalx) {
        double a = Math.log(pDeYegalSpamSachantXegalx);
        double b = Math.log(pDeYegalHamSachantXegalx);
        return a > b;
    }

    //Methode qui r�alise la formule du diapo
    static double getPdeXsachantYegalSpamOuHam(HashMap<String, Double> frequency, HashMap<String, Double> presence){
        double res = 1d;
        for(Map.Entry<String, Double> entry : frequency.entrySet()) {
            String key = entry.getKey();

            double motPresent = presence.get(key);
            if (motPresent == 1d){
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

    //Methode qui additionne message apr�s message les effectifs de presence
    static void mergeValues(HashMap<String, Double> effectif, HashMap<String, Double> vecteurx) {
        for(Map.Entry<String, Double> entry : effectif.entrySet()) {
            String key = entry.getKey();
            Double value = entry.getValue();
            effectif.put(key,value + vecteurx.get(key));
        }
    }

    //Methode qui transforme les effectifs en fr�quence avec le total de ham/spam
    static void effectifToFrequency(HashMap<String, Double> effectif, Double total) {
        for(Map.Entry<String, Double> entry : effectif.entrySet()) {
            String key = entry.getKey();
            Double value = entry.getValue()/total;
            effectif.put(key,value);
        }
    }

    //Methode qui charge le dictionnaire initial
    static String[] charger_dictionnaire(){
        try {
            Scanner sc = new Scanner(new File("dictionnaire1000en.txt"));
            int i = 0; // Le dictionnaire a 1000 mots mais peux être amen� a changer.
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

    //Methode qui g�n�re le vecteur de presence
    static HashMap<String,Double> lire_message(String[] dictionnaire, File file){
        HashMap<String,Double> res = new HashMap<>(dictionnaire.length); //On sait que l'on va utiliser uniquement les mots du dictionnaire.
        for (String mot : dictionnaire) {
            res.put(mot,0d);//par d�fault le mot ne se trouve pas dans le message, on entre simplement les cl�s.
        }

        try {
            Scanner sc = new Scanner(file);
            while (sc.hasNextLine()) {
                String[] listeMots = sc.nextLine().split("[\\s\\p{Punct}]+"); // On lis le fichier ligne apr�s ligne, et on coupe les lignes sur la ponctuation (source:https://stackoverflow.com/questions/35324047/reading-in-a-file-without-punctuation)
                for (String mot : listeMots) {                                      // Attention : on lis aussi les balises HMTL avec cette regex
                    boolean presence = res.containsKey(mot.toUpperCase()); //evite le casse
                    if (presence && res.get(mot.toUpperCase()) == 0.0){ // On a trouv� le mot du dictionnaire dans le message
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
