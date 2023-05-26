package mdpa.lnr.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import mdpa.gdpr.metamodel.GDPR.AbstractGDPRElement;
import mdpa.gdpr.metamodel.GDPR.Consent;
import mdpa.gdpr.metamodel.GDPR.Controller;
import mdpa.gdpr.metamodel.GDPR.Data;
import mdpa.gdpr.metamodel.GDPR.ExerciceOfPublicAuthrity;
import mdpa.gdpr.metamodel.GDPR.LegalBasis;
import mdpa.gdpr.metamodel.GDPR.NaturalPerson;
import mdpa.gdpr.metamodel.GDPR.PerformanceOfContract;
import mdpa.gdpr.metamodel.GDPR.PersonalData;
import mdpa.gdpr.metamodel.GDPR.Processing;
import mdpa.gdpr.metamodel.GDPR.Role;
import mdpa.gdpr.metamodel.api.GDPRMetamodelApi;
import mdpa.gdpr.metamodel.contextproperties.ContextAnnotation;
import mdpa.gdpr.metamodel.contextproperties.ContextDefinition;
import mdpa.gdpr.metamodel.contextproperties.PropertyAnnotation;
import mdpa.lnr.metamodel.LegalNormRules.LegalConsequence;
import mdpa.lnr.metamodel.LegalNormRules.LegalNormRules;
import mdpa.lnr.metamodel.LegalNormRules.LegalNormRulesFactory;
import mdpa.lnr.metamodel.LegalNormRules.LegalObligation;
import mdpa.lnr.metamodel.LegalNormRules.AND;
import mdpa.lnr.metamodel.LegalNormRules.Attribute;
import mdpa.lnr.metamodel.LegalNormRules.Connector;
import mdpa.lnr.metamodel.LegalNormRules.ContextDependentProperty;
import mdpa.lnr.metamodel.LegalNormRules.NOT;
import mdpa.lnr.metamodel.LegalNormRules.OR;
import mdpa.lnr.metamodel.LegalNormRules.Prerequisit;

public class AnalysisMain {

	private LegalNormRules lnr;
	private GDPRMetamodelApi GDPRApi;
	private GDPRElementComparator comparator;

	private void iterateLNR() {
		List<LegalObligation> resultingObligations = new ArrayList<>();
		for(LegalConsequence consequence : this.lnr.getLegalconsequence()) {
			ArrayList<List<Prerequisit>> disjunctRules = new ArrayList<>();
			flattenRuleTree(consequence.getPrerequisit(), disjunctRules);
			
			if(checkRules(disjunctRules)) {
				resultingObligations.addAll(consequence.getLegalobligation());
			}
		}
		
		//TODO: Do something with obligations or return boolean if obligation exists.
	}

	private boolean checkContext(ContextDefinition definition, Connector context) {
		context.getPrerequisit();
		return false;
	} 
	
	private void flattenRuleTree(Prerequisit prerequisit, ArrayList<List<Prerequisit>> disjunctRules) {
		if (disjunctRules.isEmpty()) {
			disjunctRules.add(new ArrayList<>());
		}
		
		if (prerequisit instanceof AND) {
			AND and = (AND) prerequisit;
			for (Prerequisit andPrerequisit : and.getPrerequisit()) {
				flattenRuleTree(andPrerequisit, disjunctRules);
			}
		
		} else if (prerequisit instanceof OR) {
			OR or = (OR) prerequisit;
			ArrayList<List<Prerequisit>> newDisjunctRuleSets = new ArrayList<>();
			for(int i = 1; i < or.getPrerequisit().size(); i++) { // skip the first disjunction as it does not require cloning.
				ArrayList<List<Prerequisit>> newDisjunctRuleSet = (ArrayList<List<Prerequisit>>) disjunctRules.clone();
				flattenRuleTree(or.getPrerequisit().get(i), newDisjunctRuleSet);
				newDisjunctRuleSets.addAll(newDisjunctRuleSet);
			}
			flattenRuleTree(or.getPrerequisit().get(0), disjunctRules);
			disjunctRules.addAll(newDisjunctRuleSets);
			
		} else if ( prerequisit instanceof NOT) {
			NOT not = (NOT) prerequisit;
			ArrayList<List<Prerequisit>> toBeNegatedDisjunctRuleSet = new ArrayList<>();
			flattenRuleTree(not.getNegatedPrerequisit(), toBeNegatedDisjunctRuleSet);
			ArrayList<List<Prerequisit>> newDisjunctRuleSets = new ArrayList<>();
			for(int i = 1; i < toBeNegatedDisjunctRuleSet.size(); i++) { // skip the first rule set to be negated as it does not require cloning.
				// 1. Create a negated equivalent of the rule.
				List<Prerequisit> negatedRule = createNegatedRule(toBeNegatedDisjunctRuleSet.get(i));
				
				// 2. As each rule in the toBeNegatedDisjunctRuleSet represents a disjunct rule part, we need to created copies of the previous
				// disjunctRules, similar to how it is done with OR but as the remaining tree has already been traversed, not in a recursive manner.
				ArrayList<List<Prerequisit>> newDisjunctRuleSet = (ArrayList<List<Prerequisit>>) disjunctRules.clone();
				appendRuleToEachDisjunctRule(negatedRule, newDisjunctRuleSet);
				newDisjunctRuleSets.addAll(newDisjunctRuleSet);
			}
			// For the first rule set to be negated, we do not clone the disjunctRules, but directly work on them.
			List<Prerequisit> negatedRule = createNegatedRule(toBeNegatedDisjunctRuleSet.get(0));
			appendRuleToEachDisjunctRule(negatedRule, disjunctRules);
			disjunctRules.addAll(newDisjunctRuleSets);
			
		} else { // Attribute, ContextDependentProperty
			for (List<Prerequisit> disjunctRule : disjunctRules) {
				disjunctRule.add(prerequisit);
			}
		}
	}
	
	private List<Prerequisit> createNegatedRule(List<Prerequisit> toBeNegatedRule) {
		List<Prerequisit> negatedRule = new ArrayList<>();
		for(Prerequisit toBeNegatedPrerequisit : toBeNegatedRule) {
			NOT negatedPrerequisit = LegalNormRulesFactory.eINSTANCE.createNOT();
			negatedPrerequisit.setNegatedPrerequisit(toBeNegatedPrerequisit);
			negatedRule.add(negatedPrerequisit);
		}
		
		return negatedRule;
	}
	
	private void appendRuleToEachDisjunctRule(List<Prerequisit> rule, ArrayList<List<Prerequisit>> disjunctRuleSet) {
		for(List<Prerequisit> disjunctRule : disjunctRuleSet) {
			disjunctRule.addAll(rule);
		}
	}
	
	private boolean checkRules(List<List<Prerequisit>> disjunctRules) {
		for (List<Prerequisit> rule : disjunctRules) {
			boolean ruleMet = true;
			List<Prerequisit> contextDependentProperties = new ArrayList<>();
			for (Prerequisit prerequisit : rule) {
				if (isContextDependent(prerequisit)) {
					contextDependentProperties.add(prerequisit);
				} else {
					if(!checkPrerequisit(prerequisit)) {
						ruleMet = false;
						break;
					}
				}
			}
			if(ruleMet) {
				//Iterate ContextDependentProperties
				// figure out dependencies between CDP
			}
			
			if(ruleMet) { // If a rule is completely met, skip checking the remaining rules and return
				return true;
			}
		}
		
		// No rule is met
		return false;
	}
	
	private void flattenPrerequisitsToContexts(Prerequisit prerequisit, ArrayList<List<AbstractGDPRElement>> contexts) {
		if(contexts.isEmpty()) {
			contexts.add(new ArrayList<>());
		}
		if (prerequisit instanceof AND) { 
			AND and = (AND) prerequisit;
			for (Prerequisit andPrerequisit : and.getPrerequisit()) {
				flattenPrerequisitsToContexts(andPrerequisit, contexts);

			}
		} else if (prerequisit instanceof OR) {
			OR or = (OR) prerequisit;
			ArrayList<List<AbstractGDPRElement>> orContexts = new ArrayList<>();
			for(int i = 1; i < or.getPrerequisit().size(); i++) {
				// für jeden eine copy erstellen uns ausführen
				ArrayList<List<AbstractGDPRElement>> clone = (ArrayList<List<AbstractGDPRElement>>) contexts.clone();
				flattenPrerequisitsToContexts(or.getPrerequisit().get(i), clone);
				orContexts.addAll(clone);
			}
			flattenPrerequisitsToContexts(or.getPrerequisit().get(0), contexts);
			contexts.addAll(orContexts);
			
		} else if (prerequisit instanceof Attribute) {
			for (List<AbstractGDPRElement> context : contexts) {
				context.add(((Attribute) prerequisit).getReferenceElement());
			}
		} else if (prerequisit instanceof ContextDependentProperty) {
			
		}
		// NOT and other ContextDependentProperties are not supported
	}

	private boolean checkPrerequisit(ContextDependentProperty contextDepProperty) {
		contextDepProperty.getPropertyValue().getParentProperty();
		contextDepProperty.getReferenceElement();
		List<PropertyAnnotation> annotations = GDPRApi.getPropertyAnnotations(contextDepProperty.getReferenceElement());
		for (PropertyAnnotation propertyAnnotation : annotations) {
			if(propertyAnnotation.getProperty().equals(contextDepProperty.getPropertyValue().getParentProperty())) {
				for (ContextAnnotation contextAnnotation : propertyAnnotation.getContextannotation()) {
					if (contextAnnotation.getPropertyvalue().contains(contextDepProperty.getPropertyValue())) {
						contextAnnotation.getContextdefinition();
						//TODO: compare
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	private boolean checkPrerequisit(Attribute attribute) {
		return comparator.compareGDPRElements(attribute.getReferenceElement(), attribute.getPresumedState());
	}

	private boolean checkPrerequisit(AND and) {
		List<Prerequisit> contextDependentPrerequisites = new ArrayList<>();
		for(Prerequisit connectedElement : and.getPrerequisit()) {
			if (isContextDependent(connectedElement)) {
				contextDependentPrerequisites.add(connectedElement);
			} else {
				
				if(!checkPrerequisit(connectedElement)) {
					return false;
				}
			}
		}
		
		for(Prerequisit contextDependentPrerequisit : contextDependentPrerequisites) {
			checkPrerequisit(contextDependentPrerequisit);
		}
		return true;
	}

	private boolean checkPrerequisitg(OR or) {
		for(Prerequisit connectedElement : or.getPrerequisit()) {
			if(checkPrerequisit(connectedElement)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean checkPrerequisit(NOT not) {
		return !checkPrerequisit(not.getNegatedPrerequisit());
	}
	
	private boolean checkPrerequisit(Prerequisit prerequisit) {
		if (prerequisit instanceof NOT) {
			return checkPrerequisit((NOT) prerequisit);
		} else if (prerequisit instanceof Attribute) {
			return checkPrerequisit((Attribute) prerequisit);
		} else if (prerequisit instanceof ContextDependentProperty) {
			return checkPrerequisit((ContextDependentProperty) prerequisit);
		} else {
			System.err.println("Creation of disjunct rule sets failed.");
			return false;
		}
	}
	
	private boolean isContextDependent(Prerequisit prerequisit) {
		Prerequisit elementToCheck = prerequisit;
		if (prerequisit instanceof NOT) {
			elementToCheck = ((NOT) prerequisit).getNegatedPrerequisit();
		}
		if (elementToCheck instanceof ContextDependentProperty) {
			return true;
		}
		return false;
	}
}
