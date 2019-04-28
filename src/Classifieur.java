import java.io.Serializable;
import java.util.HashMap;

public class Classifieur implements Serializable{

	private static final long serialVersionUID = 1L;

	HashMap<String,Double> probaSpam;
	HashMap<String,Double> probaHam;
	double pYegalSpam;
	double pYegalHam;
	double nbrSpam;
	double nbrHam;
	
	public Classifieur(HashMap<String,Double> pS,HashMap<String,Double> pH,double pYS,double pYH, double nS, double nH) {
		this.probaHam = pH;
		this.probaSpam = pS;
		this.pYegalHam = pYH;
		this.pYegalSpam = pYS;
		this.nbrSpam = nS;
		this.nbrHam = nH;
	}
}
