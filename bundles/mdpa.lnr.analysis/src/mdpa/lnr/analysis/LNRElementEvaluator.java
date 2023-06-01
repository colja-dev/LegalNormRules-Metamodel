package mdpa.lnr.analysis;

import java.util.ArrayList;
import java.util.List;

import mdpa.gdpr.metamodel.GDPR.AbstractGDPRElement;
import mdpa.gdpr.metamodel.api.GDPRMetamodelApi;
import mdpa.gdpr.metamodel.contextproperties.ContextAnnotation;
import mdpa.gdpr.metamodel.contextproperties.ContextDefinition;
import mdpa.gdpr.metamodel.contextproperties.ContextDependentContextElement;
import mdpa.gdpr.metamodel.contextproperties.GDPRContextElement;
import mdpa.gdpr.metamodel.contextproperties.PropertyAnnotation;
import mdpa.lnr.metamodel.LegalNormRules.AND;
import mdpa.lnr.metamodel.LegalNormRules.Attribute;
import mdpa.lnr.metamodel.LegalNormRules.ContextDependentProperty;
import mdpa.lnr.metamodel.LegalNormRules.LegalNormRulesFactory;
import mdpa.lnr.metamodel.LegalNormRules.NOT;
import mdpa.lnr.metamodel.LegalNormRules.OR;
import mdpa.lnr.metamodel.LegalNormRules.Prerequisit;

public class LNRElementEvaluator {
	
	private GDPRMetamodelApi GDPRApi;
	private GDPRElementSimilarityComparator comparator;
	
	public LNRElementEvaluator(GDPRMetamodelApi GDPRApi, GDPRElementSimilarityComparator comparator) {
		this.GDPRApi = GDPRApi;
		this.comparator = comparator;
	}
	
	public boolean checkRules(List<List<Prerequisit>> disjunctRules) {
		for (List<Prerequisit> rule : disjunctRules) {
			boolean ruleMet = true;
			List<Prerequisit> contextDependentProperties = new ArrayList<>();
			for (Prerequisit prerequisit : rule) {
				if (isContextDependent(prerequisit)) {
					contextDependentProperties.add(prerequisit);
				} else {
					if(!checkIndependentPrerequisit(prerequisit)) {
						ruleMet = false;
						break;
					}
				}
			}
			// figure out dependencies between CDP
			if(ruleMet) {
				for (Prerequisit contextDependentPrerequisit : contextDependentProperties) {
					ContextDependentProperty cdp = (ContextDependentProperty)contextDependentPrerequisit;
					//TODO: check whether this if is right
					if(!checkContextDependentPrerequisit(cdp, rule)) {
						ruleMet = false;
						break;
					}
				}
			}
			if(ruleMet) { // If a rule is completely met, skip checking the remaining rules and return
				return true;
			}
		}
		// No rule is met
		return false;
	}
	
	public boolean checkIndependentPrerequisit(Prerequisit prerequisit) {
		if (prerequisit instanceof NOT) {
			return checkIndependentPrerequisit((NOT) prerequisit);
		} else if (prerequisit instanceof Attribute) {
			return checkIndependentPrerequisit((Attribute) prerequisit);
		} else {
			System.err.println("Creation of disjunct rule sets failed.");
			return false;
		}
	}
	
	public boolean checkContextDependentPrerequisit(Prerequisit prerequisit, List<Prerequisit> context) {
		if (prerequisit instanceof NOT) {
			return checkContextDependentPrerequisit((NOT) prerequisit, context);
		} else if (prerequisit instanceof ContextDependentProperty) {
			return checkContextDependentPrerequisit((ContextDependentProperty) prerequisit, context);
		} else {
			System.err.println("Error checking context dependent prerequisit.");
			return false;
		}
	}
	
	public void flattenRuleTree(Prerequisit prerequisit, ArrayList<List<Prerequisit>> disjunctRules) {
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

	private boolean comparePropertyAnnotationAndContextDependentProperty(PropertyAnnotation propertyAnnotation, ContextDependentProperty contextDependentProperty, List<Prerequisit> context) {
		// If the annotated property is the same as the parent property of the value of the context dependent property definition
		if(propertyAnnotation.getProperty().equals(contextDependentProperty.getPropertyValue().getParentProperty())) {
			// iterate over all context annotations and find the ones that annotate the same value as in the context dependent property definition
			for (ContextAnnotation contextAnnotation : propertyAnnotation.getContextannotation()) {
				if (contextAnnotation.getPropertyvalue().contains(contextDependentProperty.getPropertyValue())) {
					// first compare the GDPR-Elements of the context and then the other referenced context annotations
					for (ContextDefinition contextDefinition : contextAnnotation.getContextdefinition()) {
						if (compareContextDefinitionToContext(contextDefinition, context)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	// The context list of prerequisits cannot contain AND or OR and should be one of the results of flattenRuleTree!
	private boolean compareContextDefinitionToContext(ContextDefinition contextDef, List<Prerequisit> context) {

		List<Prerequisit> gdprContextElements = new ArrayList<>();
		List<Prerequisit> contextDependentContextElements = new ArrayList<>();
		for (Prerequisit contextElement : context) {
			if (isContextDependent(contextElement)) {
				contextDependentContextElements.add(contextElement);
			} else {
				gdprContextElements.add(contextElement);
			}
		}
		
		for(GDPRContextElement gdprContextDefElement : contextDef.getGdprElements()) {
			boolean contextElementMet = false;
			for (Prerequisit gdprContextElement : gdprContextElements) {
				if(compareGDPRContextElementAndPrerequisit(gdprContextDefElement, gdprContextElement)) {
					contextElementMet = true;
					break;
				}
			}
			if(!contextElementMet) {
				return false;
			}
		}
		
		for(ContextDependentContextElement contextDepContextDefElement : contextDef.getContextDependentProperties()) {
			boolean contextElementMet = false;
			for (Prerequisit contextDepContextElement : contextDependentContextElements) {
				if(compareContextDependentContextElementAndPrerequisit(contextDepContextDefElement, contextDepContextElement, context)) {
					contextElementMet = true;
					break;
				}
			}
			if(!contextElementMet) {
				return false;
			}
		}
		
		return true;
	}
	
	private boolean compareContextDependentContextElementAndPrerequisit(ContextDependentContextElement contextDepContextElement, Prerequisit prerequisit, List<Prerequisit> context) {
		Prerequisit comparePrerequisit = prerequisit;
		if (contextDepContextElement.isNegated()) {
			if (prerequisit instanceof NOT) {
				NOT not = (NOT) prerequisit;
				comparePrerequisit = not.getNegatedPrerequisit();
			} else {
				return false;
			}
		} else {
			if (prerequisit instanceof NOT) {
				return false;
			}
		}
		
		return compareContextDependentContextPropertyAndPrerequisit(contextDepContextElement.getContextDependentProperty(), comparePrerequisit, context);
	}
	
	private boolean compareContextDependentContextPropertyAndPrerequisit(PropertyAnnotation propertyAnnotation, Prerequisit prerequisit, List<Prerequisit> context) {
		if(prerequisit instanceof ContextDependentProperty) {
			ContextDependentProperty contextDepProperty = (ContextDependentProperty) prerequisit;
			return comparePropertyAnnotationAndContextDependentProperty(propertyAnnotation, contextDepProperty, context);
		} else {
			System.err.println("compareContextDefinitionToContext called with invalid context. flattenPrerequisitsToContexts should be called first.");
			return false;
		}
	}
	
	private boolean compareGDPRContextElementAndPrerequisit(GDPRContextElement gdprContextElement, Prerequisit prerequisit) {
		// if the context element of the context definition is negated an explicitly also negated prerequisit is required
		Prerequisit comparePrerequisit = prerequisit;
		if (gdprContextElement.isNegated()) {
			if (prerequisit instanceof NOT) {
				NOT not = (NOT) prerequisit;
				comparePrerequisit = not.getNegatedPrerequisit();
			} else {
				return false;
			}
		} else {
			if (prerequisit instanceof NOT) {
				return false;
			}
		}
		
		return compareGDPRElementAndPrerequisit(gdprContextElement.getGdprElement(), comparePrerequisit);
	}
	
	private boolean compareGDPRElementAndPrerequisit(AbstractGDPRElement gdprElement, Prerequisit prerequisit) {
		// just to be sure
		if (prerequisit instanceof Attribute) {
			Attribute attribute = (Attribute) prerequisit;
			return comparator.compareGDPRElements(attribute.getReferenceElement(), gdprElement);
		} else {
			System.err.println("compareContextDefinitionToContext called with invalid context. flattenPrerequisitsToContexts should be called first.");
			return false;
		}
	}
	
	private boolean checkIndependentPrerequisit(Attribute attribute) {
		return comparator.compareGDPRElements(attribute.getReferenceElement(), attribute.getPresumedState());
	}
	
	private boolean checkContextDependentPrerequisit(ContextDependentProperty contextDependentProperty, List<Prerequisit> context) {		
		// Get all annotations of the referenced element
		List<PropertyAnnotation> annotations = GDPRApi.getPropertyAnnotations(contextDependentProperty.getReferenceElement());
		for (PropertyAnnotation propertyAnnotation : annotations) {
			if(comparePropertyAnnotationAndContextDependentProperty(propertyAnnotation, contextDependentProperty, context)) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean checkContextDependentPrerequisit(NOT not, List<Prerequisit> context) {
		return !checkContextDependentPrerequisit(not.getNegatedPrerequisit(), context);
	}
	
	private boolean checkIndependentPrerequisit(NOT not) {
		return !checkIndependentPrerequisit(not.getNegatedPrerequisit());
	}

	private boolean checkIndependentPrerequisit(AND and) {
		List<Prerequisit> contextDependentPrerequisites = new ArrayList<>();
		for(Prerequisit connectedElement : and.getPrerequisit()) {
			if (isContextDependent(connectedElement)) {
				contextDependentPrerequisites.add(connectedElement);
			} else {
				
				if(!checkIndependentPrerequisit(connectedElement)) {
					return false;
				}
			}
		}
		
		for(Prerequisit contextDependentPrerequisit : contextDependentPrerequisites) {
			checkIndependentPrerequisit(contextDependentPrerequisit);
		}
		return true;
	}

	private boolean checkIndependentPrerequisitg(OR or) {
		for(Prerequisit connectedElement : or.getPrerequisit()) {
			if(checkIndependentPrerequisit(connectedElement)) {
				return true;
			}
		}
		return false;
	}
}
