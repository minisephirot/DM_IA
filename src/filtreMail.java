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
        String[] dictionnaire = filtreAntiSpam.charger_dictionnaire();
        System.out.println("Dictionnaire chargé. "+dictionnaire.length+" mots ont été enregistrés.");
        if (debug) System.out.println("Liste des mots :"+Arrays.asList(dictionnaire)+"\n");

        HashMap<String,Double> vecteurx =  filtreAntiSpam.lire_message(dictionnaire,new File(args[1]));

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

        double pXegalxSachantYegalSpam = filtreAntiSpam.getPdeXsachantYegalSpamOuHam(classifieur.probaSpam,vecteurx);
        double pXegalxSachantYegalHam = filtreAntiSpam.getPdeXsachantYegalSpamOuHam(classifieur.probaHam,vecteurx);
        double pDeXetYegalSpam = pXegalxSachantYegalSpam * classifieur.pYegalSpam;
        double pDeXetYegalHam = pXegalxSachantYegalHam * classifieur.pYegalHam;
        double pDeXegalx = pDeXetYegalHam + pDeXetYegalSpam;

        double pDeYegalSpamSachantXegalx = (1d/pDeXegalx) * classifieur.pYegalSpam * pXegalxSachantYegalSpam ;
        double pDeYegalHamSachantXegalx = (1d/pDeXegalx) * classifieur.pYegalHam * pXegalxSachantYegalHam;

        boolean isSpam = filtreAntiSpam.isSpam(pDeYegalSpamSachantXegalx,pDeYegalHamSachantXegalx);
        System.out.println("P(Y=SPAM | X=x) = "+pDeYegalSpamSachantXegalx+", P(Y=HAM | X=x) = "+pDeYegalHamSachantXegalx);
        System.out.print("D'apres ' "+ args[0]+"', le message '"+args[1]+"' est un ");
        if (isSpam){
            System.out.print("SPAM !\n");
        }else{
            System.out.print("HAM ! \n");
        }
    }

  }
