import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class apprendFiltreLigne {

	 private static final boolean debug = false; // editer vrai si on veux voir des infos de debug

	    public static void main(String args[]) {
	        //Init
	        if (args.length != 3 || !(args[2].equals("SPAM")||args[2].equals("HAM")) ) {
	            System.out.println("Utilisation de apprendFiltreLigne : 'nom du classifieur a modifier' 'message a ajouter' 'type de message (HAM|SPAM)");
	            System.exit(1);
	        }
	        
	        
	        System.out.println("Chargement du dictionnaire...");
	        String[] dictionnaire = filtreAntiSpam.charger_dictionnaire();
	        System.out.println("Dictionnaire charg�. "+dictionnaire.length+" mots ont �t� enregistr�s.");
	        if (debug) System.out.println("Liste des mots :"+Arrays.asList(dictionnaire)+"\n");

	        File fichier =  new File(args[0]) ;
	        // ouverture d'un flux sur un fichier
	        ObjectInputStream ois;
	        Classifieur classifieur = null;
	        try {
	            ois = new ObjectInputStream(new FileInputStream(fichier));
	            // d�s�rialization de l'objet
	            classifieur = (Classifieur)ois.readObject() ;
	            ois.close();
	        } catch (FileNotFoundException e) {
	            e.printStackTrace();
	            System.out.println("Classifieur non trouv�");
	            System.exit(1);
	        } catch (IOException | ClassNotFoundException e) {
	            e.printStackTrace();
	            System.exit(1);
	        }

	        HashMap<String, Double> vecteurx = filtreAntiSpam.lire_message(dictionnaire, new File(args[1]));
	        String key;
	        Double value;
	        if(args[2].equals("SPAM")) {
	        	//inverse de filtreAntiSpam.effectifToFrequency(classifieur.probaSpam, classifieur.nbrSpam + 2) pour revenir a l'effectif
	        	for(Map.Entry<String, Double> entry : classifieur.probaSpam.entrySet()) {
	                key = entry.getKey();
	                value = entry.getValue()*classifieur.nbrSpam + 2;
	                classifieur.probaSpam.put(key,value);
	            }
	        	//ajout du message
	        	classifieur.nbrSpam += 1;
	        	filtreAntiSpam.mergeValues(classifieur.probaSpam, vecteurx);
	        	//retour a la frequence
	        	filtreAntiSpam.effectifToFrequency(classifieur.probaSpam, classifieur.nbrSpam + 2);
	        }else {
	        	//inverse de filtreAntiSpam.effectifToFrequency(classifieur.probaHam, classifieur.nbrHam) pour revenir a l'effectif
	        	for(Map.Entry<String, Double> entry : classifieur.probaHam.entrySet()) {
	                key = entry.getKey();
	                value = entry.getValue()*classifieur.nbrHam;
	                classifieur.probaHam.put(key,value);
	            }
	        	//ajout du message
	        	classifieur.nbrHam += 1;
	        	filtreAntiSpam.mergeValues(classifieur.probaHam, vecteurx);
	        	//retour a la frequence
	        	filtreAntiSpam.effectifToFrequency(classifieur.probaHam, classifieur.nbrHam);
	        }
  
	        if (debug) {
	            System.out.println("\nFrequence d'apparition des mots (spam) :" + Collections.singletonList(classifieur.probaSpam));
	            System.out.println("Frequence d'apparition des mots (ham) :" + Collections.singletonList(classifieur.probaHam));
	        }
	        
	      //On a besoin de p(Y=SPAM) et p(Y=HAM)
	        classifieur.pYegalSpam = classifieur.nbrSpam / (classifieur.nbrSpam + classifieur.nbrHam);
	        classifieur.pYegalHam = 1d - classifieur.pYegalSpam;
	        if (debug)
	            System.out.println("Probabilit� qu'un message soit un spam vs Probabilit� qu'un message soit un ham = " + classifieur.pYegalSpam + " contre " + classifieur.pYegalHam);

	        System.out.println("Serialization...");
	        File out = new File(args[0]);

	        // ouverture d'un flux sur un fichier
	        ObjectOutputStream oos;
	        try {
	            oos = new ObjectOutputStream(new FileOutputStream(out));
	            // s�rialization de l'objet
	            oos.writeObject(classifieur);
	            oos.close();
	        } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	            System.exit(1);
	        }
	        System.out.println("Modification de " + args[0] + " termin�e (ajout de "+args[1]+" en tant que "+args[2] +")");
	    }
}
