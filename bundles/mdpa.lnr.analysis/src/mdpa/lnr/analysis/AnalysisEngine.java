package mdpa.lnr.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.common.util.URI;

import mdpa.gdpr.metamodel.api.GDPRMetamodelApi;
import mdpa.lnr.metamodel.LegalNormRules.LegalConsequence;
import mdpa.lnr.metamodel.LegalNormRules.LegalObligation;
import mdpa.lnr.metamodel.LegalNormRules.Prerequisit;
import mdpa.lnr.metamodel.api.LegalNormRulesApi;

public class AnalysisEngine {
	
	private LegalNormRulesApi LNRApi;
	private LNRElementEvaluator lnrEvaluator;
	private List<LegalObligation> resultingLegalObligations = new ArrayList<>();
	
	public AnalysisEngine(URI GDPRModelURI, URI ContextPropertiesURI, URI LNRModelURI) {
		System.out.println("Initialize and load resources...");
		GDPRMetamodelApi gdprApi = new GDPRMetamodelApi(GDPRModelURI, Optional.of(ContextPropertiesURI));
		this.lnrEvaluator = new LNRElementEvaluator(gdprApi, new GDPRElementSimilarityComparator(gdprApi));
		this.LNRApi = new LegalNormRulesApi(LNRModelURI);
		
	}
	
	
	// flatten Rule Tree -> checkRules( checkIndependentPrerequisits -> checkContextDependentPrerequisits(
	public void analyze() {
		System.out.println("Starting analysis...");
		for(LegalConsequence consequence : this.LNRApi.getLegalNormRules().getLegalconsequence()) {
			ArrayList<List<Prerequisit>> disjunctRules = new ArrayList<>();
			System.out.println("Flattening rule tree for consequence " + consequence.getEntityName());
			this.lnrEvaluator.flattenRuleTree(consequence.getPrerequisit(), disjunctRules);
			
			System.out.println("Checking rules of consequence " + consequence.getEntityName());
			if(this.lnrEvaluator.checkRules(disjunctRules)) {
				this.resultingLegalObligations.addAll(consequence.getLegalobligation());
			}
		}
		
		// Output for debugging reasons.
		if(this.resultingLegalObligations.isEmpty()) {
			System.out.println("No resulting legal obligations.");
		} else {
			System.out.println("There exist resulting legal obligations.");
		}
		
		//TODO: Do something with obligations or return boolean if obligation exists.
	}
	
	public boolean obligationsExist() {
		return !resultingLegalObligations.isEmpty();
	}

}
