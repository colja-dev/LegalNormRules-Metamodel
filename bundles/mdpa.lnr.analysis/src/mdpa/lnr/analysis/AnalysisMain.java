package mdpa.lnr.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import mdpa.gdpr.metamodel.api.GDPRMetamodelApi;
import mdpa.lnr.metamodel.LegalNormRules.LegalConsequence;
import mdpa.lnr.metamodel.LegalNormRules.LegalObligation;
import mdpa.lnr.metamodel.LegalNormRules.Prerequisit;
import mdpa.lnr.metamodel.api.LegalNormRulesApi;

public class AnalysisMain {
	
	private LegalNormRulesApi LNRApi;
	private LNRElementEvaluator lnrEvaluator;
	private List<LegalObligation> resultingLegalObligations = new ArrayList<>();
	
	public AnalysisMain(String GDPRModelPath, String ContextPropertiesPath, String LNRModelPath) {
		this.LNRApi = new LegalNormRulesApi(LNRModelPath);
		GDPRMetamodelApi gdprApi = new GDPRMetamodelApi(GDPRModelPath, Optional.of(ContextPropertiesPath));
		this.lnrEvaluator = new LNRElementEvaluator(gdprApi, new GDPRElementSimilarityComparator(gdprApi));
	}
	
	
	// flatten Rule Tree -> checkRules( checkIndependentPrerequisits -> checkContextDependentPrerequisits(
	public void analyze() {
		for(LegalConsequence consequence : this.LNRApi.getLegalNormRules().getLegalconsequence()) {
			ArrayList<List<Prerequisit>> disjunctRules = new ArrayList<>();
			this.lnrEvaluator.flattenRuleTree(consequence.getPrerequisit(), disjunctRules);
			
			if(this.lnrEvaluator.checkRules(disjunctRules)) {
				this.resultingLegalObligations.addAll(consequence.getLegalobligation());
			}
		}
		
		//TODO: Do something with obligations or return boolean if obligation exists.
	}
	
	public boolean obligationsExist() {
		return !resultingLegalObligations.isEmpty();
	}

}
