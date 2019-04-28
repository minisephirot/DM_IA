import java.io.*;
import java.util.*;

public class apprendFiltre {

    private static final boolean debug = false; // editer vrai si on veux voir des infos de debug

    public static void main(String args[]) {
        //Init
        if (args.length != 4) {
            System.out.println("Utilisation de apprendFiltre : 'nom du fichier sortie' 'dossier contenant la base d'apprentissage' 'nb de spam de la base d'apprentissage' 'nb de ham de la base d'apprentissage'");
            System.exit(1);
        }

        //Dictionnaire
        System.out.println("Chargement du dictionnaire...");
        String[] dictionnaire = filtreAntiSpam.charger_dictionnaire();
        System.out.println("Dictionnaire chargé. " + dictionnaire.length + " mots ont été enregistrés.");
        if (debug) System.out.println("Liste des mots :" + Arrays.asList(dictionnaire) + "\n");

        //Apprentissage
        double nbham = Objects.requireNonNull(new File(args[1] + "/ham").list()).length;
        double nbspam = Objects.requireNonNull(new File(args[1] + "/spam").list()).length;
        if (Integer.parseInt(args[2]) < nbspam && Integer.parseInt(args[3]) < nbham) {
            nbspam = Integer.parseInt(args[2]);
            nbham = Integer.parseInt(args[3]);
        }

        System.out.println("Apprentissage...");

        //Création des b_spam et b_ham
        HashMap<String, Double> probaSpam = new HashMap<>(dictionnaire.length); //On sait que l'on va utiliser uniquement les mots du dictionnaire.
        for (String mot : dictionnaire) {
            probaSpam.put(mot, 1d);//On a un lissage des parametres avec e = 1.
        }
        HashMap<String, Double> probaHam = new HashMap<>(dictionnaire.length); //On sait que l'on va utiliser uniquement les mots du dictionnaire.
        for (String mot : dictionnaire) {
            probaHam.put(mot, 0d);//Le lissage ne s'applique pas pour ham.
        }
        if (debug) {
            System.out.println("Init des proba de spam :" + Collections.singletonList(probaSpam));
            System.out.println("Init des proba de ham :" + Collections.singletonList(probaHam));
        }

        String apprentissage = args[1];
        //Apprentissage des SPAM:
        for (int i = 0; i < nbspam; i++) {
            HashMap<String, Double> vecteurx = filtreAntiSpam.lire_message(dictionnaire, new File(apprentissage + "/spam/" + i + ".txt"));
            filtreAntiSpam.mergeValues(probaSpam, vecteurx);
        }
        //Apprentissage des HAM:
        for (int i = 0; i < nbham; i++) {
            HashMap<String, Double> vecteurx = filtreAntiSpam.lire_message(dictionnaire, new File(apprentissage + "/ham/" + i + ".txt"));
            filtreAntiSpam.mergeValues(probaHam, vecteurx);
        }
        if (debug) {
            System.out.println("Effectif des mots après lecture des spam :" + Collections.singletonList(probaSpam));
            System.out.println("Effectif des mots après lecture des ham :" + Collections.singletonList(probaHam));
        }
        //On a compté l'effectif d'apparition des mots dans les 2 catégories, on doit maintenant diviser ces effectifs par
        //Leurs nombre respectif de spam/ham avec +2 pour les spam car nous lissons ces probabilitées.
        filtreAntiSpam.effectifToFrequency(probaSpam, nbspam + 2);
        filtreAntiSpam.effectifToFrequency(probaHam, nbham);
        if (debug) {
            System.out.println("\nFrequence d'apparition des mots (spam) :" + Collections.singletonList(probaSpam));
            System.out.println("Frequence d'apparition des mots (ham) :" + Collections.singletonList(probaHam));
        }

        //On a besoin de p(Y=SPAM) et p(Y=HAM)
        double pYegalSpam = nbspam / (nbspam + nbham);
        double pYegalHam = 1d - pYegalSpam;
        if (debug)
            System.out.println("Probabilité qu'un message soit un spam vs Probabilité qu'un message soit un ham = " + pYegalSpam + " contre " + pYegalHam);

        System.out.println("Serialization...");
        Classifieur classifieur = new Classifieur(probaSpam, probaHam, pYegalSpam, pYegalHam, nbspam, nbham);
        File fichier = new File(args[0] + ".ser");

        // ouverture d'un flux sur un fichier
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(fichier));
            // sérialization de l'objet
            oos.writeObject(classifieur);
            oos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Creation de " + args[0] + ".ser terminée");
    }
}
