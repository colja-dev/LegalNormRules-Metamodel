package mdpa.lnr.analysis;

import java.util.ArrayList;
import java.util.List;

import mdpa.gdpr.metamodel.GDPR.AbstractGDPRElement;
import mdpa.gdpr.metamodel.GDPR.Consent;
import mdpa.gdpr.metamodel.GDPR.Data;
import mdpa.gdpr.metamodel.GDPR.ExerciceOfPublicAuthrity;
import mdpa.gdpr.metamodel.GDPR.LegalBasis;
import mdpa.gdpr.metamodel.GDPR.PerformanceOfContract;
import mdpa.gdpr.metamodel.GDPR.PersonalData;
import mdpa.gdpr.metamodel.GDPR.Processing;
import mdpa.gdpr.metamodel.GDPR.Purpose;
import mdpa.gdpr.metamodel.GDPR.Role;
import mdpa.gdpr.metamodel.api.GDPRMetamodelApi;

/**
 * Provides functionality to compare elements of the GDPR metamodel and find similar elements from the LNR rule definition.
 * 
 * This functionality should normally be provided by the classes themselfs.
 * However, extending/adding functions/methods in EMF is tedious and offers no proper editor support for code.
 * As the similarity functions might change often in future development, this way is less error prone.
 * 
 * @author Nicolas Boltz
 *
 */
public class GDPRElementComparator {

	private GDPRMetamodelApi GDPRApi;

	public GDPRElementComparator(GDPRMetamodelApi api) {
		this.GDPRApi = api;
	}
	
	public boolean compareGDPRElements(AbstractGDPRElement element, AbstractGDPRElement presumedElement) {
		if(!element.getClass().equals(presumedElement.getClass())) { // have to be of the same type
			return false;
		}
		// Aligned to how it is done in Xtend. It is easy, even though it is not good style.
		if (element instanceof Processing) {
			return compareProcessing((Processing) element, (Processing) presumedElement);
		} else if (element instanceof Data) {
			return compareData((Data) element, (Data) presumedElement);
		} else if (element instanceof Purpose) {
			return comparePurpose((Purpose) element, (Purpose) presumedElement);
		} else if (element instanceof LegalBasis) {
			return compareLegalBasis((LegalBasis) element, (LegalBasis) presumedElement);
		} else if (element instanceof Role) {
			return compareRole((Role) element, (Role) element);
		} else {
			return compareAbstractGDPRElements(element, presumedElement);
		}
	}

	private boolean checkProcessingContainment(List<Processing> container, List<Processing> contained) {
		for(Processing processing : contained) {
			List<Processing> containedProcessingEquivalent = findEquivalentInGDPRModel(processing);
			if(!containedProcessingEquivalent.isEmpty()) {
				if(!checkContainment(container, containedProcessingEquivalent)) { // if the equivalent is not contained in container
					return false;
				}
			} else { // if there is no equivalent in the gdpr model
				return false;
			}
		}

		return true;
	}

	private boolean checkPurposeContainment(List<Purpose> container, List<Purpose> contained) {
		for(Purpose purpose : contained) {
			List<Purpose> containedPurposeEquivalents = findEquivalentInGDPRModel(purpose);
			if(!containedPurposeEquivalents.isEmpty()) {
				if(!checkContainment(container, containedPurposeEquivalents)) { // if the equivalent is not contained in container
					return false;
				}
			} else { // if there is no equivalent in the gdpr model
				return false;
			}
		}

		return true;
	}

	private boolean checkLegalBasisContainment(List<LegalBasis> container, List<LegalBasis> contained) {
		for(LegalBasis containedBasis : contained) {
			List<? extends LegalBasis> containedLegalBasisEquivalents = new ArrayList<>();
			if(containedBasis instanceof PerformanceOfContract) {
				containedLegalBasisEquivalents = findEquivalentsInGDPRModel((PerformanceOfContract) containedBasis);
			} else if(containedBasis instanceof ExerciceOfPublicAuthrity) {
				containedLegalBasisEquivalents = findEquivalentsInGDPRModel((ExerciceOfPublicAuthrity) containedBasis);
			} else if(containedBasis instanceof Consent) {
				containedLegalBasisEquivalents = findEquivalentsInGDPRModel((Consent) containedBasis);
			}

			if(!containedLegalBasisEquivalents.isEmpty()) {
				if(!checkContainment(container, containedLegalBasisEquivalents)) { // if the equivalent is not contained in container
					return false;
				}
			} else {
				return false;
			}
		}

		return true;
	}

	private boolean checkInvolvedPartyContainment(List<? extends Role> container, List<? extends Role> contained) {
		for(Role containedPerson : contained) {
			List<Role> containedPersonEquivalents = findEquivalentsInGDPRModel(containedPerson);
			if(!containedPersonEquivalents.isEmpty()) {
				if(!checkContainment(container, containedPersonEquivalents)) { // if the equivalent is not contained in container
					return false;
				}
			} else { // if there is no equivalent in the gdpr model
				return false;
			}
		}
		return true;
	}

	private boolean checkDataContainment(List<Data> container, List<Data> contained) {
		for(Data containedData : contained) {
			List<? extends Data> containedDataEquivalents = new ArrayList<>();
			if(containedData instanceof PersonalData) {
				containedDataEquivalents = findEquivalentsInGDPRModel((PersonalData) containedData);
			} else {
				containedDataEquivalents = findEquivalentsInGDPRModel(containedData);
			}

			if(!containedDataEquivalents.isEmpty()) {
				if(!checkContainment(container, containedDataEquivalents)) { // if the equivalent is not contained in container
					return false;
				}
			} else { // if there is no equivalent in the gdpr model
				return false;
			}
		}
		return true;
	}

	private boolean checkContainment(List<? extends AbstractGDPRElement> container, List<? extends AbstractGDPRElement> contained) {
		for(AbstractGDPRElement containedPersonEquivalent : contained) {
			if(!container.contains(containedPersonEquivalent)) {
				return false;
			}
		}

		return true;
	}

	private List<Processing> findEquivalentInGDPRModel(Processing presumedProcessing) {
		List<Processing> equivalents = new ArrayList<>();
		for(Processing processing : GDPRApi.getLegalAssessmentFacts().getProcessing()) {		
			if(compareProcessing(processing, presumedProcessing)) {
				equivalents.add(processing);
			}
		}
		return equivalents;
	}

	private List<Purpose> findEquivalentInGDPRModel(Purpose presumedPurpose) {
		List<Purpose> equivalents = new ArrayList<>();
		for(Purpose purpose : GDPRApi.getPurposes()) {
			if(comparePurpose(purpose, presumedPurpose)) {
				equivalents.add(purpose);
			}
		}

		return equivalents;
	}

	private List<LegalBasis> findEquivalentsInGDPRModel(LegalBasis presumedBasis) {
		List<LegalBasis> equivalents = new ArrayList<>();
		for(LegalBasis basis : GDPRApi.getLegalBases()) {
			if(compareLegalBasis(basis, presumedBasis)) {
				equivalents.add(basis);
			}
		}

		return equivalents;
	}

	private List<PerformanceOfContract> findEquivalentsInGDPRModel(PerformanceOfContract presumedBasis) {
		List<PerformanceOfContract> equivalents = new ArrayList<>();
		for(LegalBasis basis : GDPRApi.getLegalBases()) {
			if(basis instanceof PerformanceOfContract) {
				PerformanceOfContract performanceOfContract = (PerformanceOfContract) basis;
				if(comparePerformanceOfContract(performanceOfContract, presumedBasis)) {
					equivalents.add(performanceOfContract);
				}
			}
		}

		return equivalents;
	}

	private List<ExerciceOfPublicAuthrity> findEquivalentsInGDPRModel(ExerciceOfPublicAuthrity presumedBasis) {
		List<ExerciceOfPublicAuthrity> equivalents = new ArrayList<>();
		for(LegalBasis basis : GDPRApi.getLegalBases()) {
			if(basis instanceof ExerciceOfPublicAuthrity) {
				ExerciceOfPublicAuthrity exerciceOfPublicAuthrity = (ExerciceOfPublicAuthrity) basis;
				if(compareLegalBasis(exerciceOfPublicAuthrity, presumedBasis)) {
					equivalents.add(exerciceOfPublicAuthrity);
				}
			}
		}

		return equivalents;
	}

	private List<Consent> findEquivalentsInGDPRModel(Consent presumedBasis) {
		List<Consent> equivalents = new ArrayList<>();
		for(LegalBasis basis : GDPRApi.getLegalBases()) {
			if(basis instanceof Consent) {
				Consent consent = (Consent) basis;
				if(compareConsent(consent, presumedBasis)) {
					equivalents.add(consent);
				}
			}
		}

		return equivalents;
	}

	private List<Data> findEquivalentsInGDPRModel(Data presumedData) {
		List<Data> equivalents = new ArrayList<>();
		for(Data data : GDPRApi.getLegalAssessmentFacts().getData()) {
			if(compareData(data, presumedData)) {
				equivalents.add(data);
			}
		}
		return equivalents;
	}

	private List<PersonalData> findEquivalentsInGDPRModel(PersonalData presumedPersData) {
		List<PersonalData> equivalents = new ArrayList<>();
		for(Data data : GDPRApi.getLegalAssessmentFacts().getData()) {
			if(data instanceof PersonalData) {
				PersonalData persData = (PersonalData) data;
				if(comparePersonalData(persData, presumedPersData)) {
					equivalents.add(persData);
				}
			}
		}

		return equivalents;
	}

	private List<Role> findEquivalentsInGDPRModel(Role presumedParty) {
		List<Role> equivalents = new ArrayList<>();
		for(Role involvedParty : GDPRApi.getInvolvedParties()) {
			if(compareRole(involvedParty, presumedParty)) {
				equivalents.add(involvedParty);
			}
		}

		return equivalents;
	}

	private boolean compareProcessing(Processing processing, Processing presumedProcessing) {
		if (compareAbstractGDPRElements(processing, presumedProcessing) || 
				checkDataContainment(processing.getInputData(), presumedProcessing.getInputData()) ||
				checkDataContainment(processing.getOutputData(), presumedProcessing.getOutputData()) ||
				checkPurposeContainment(processing.getPurpose(), presumedProcessing.getPurpose()) ||
				checkLegalBasisContainment(processing.getOnTheBasisOf(), processing.getOnTheBasisOf()) ||
				checkProcessingContainment(processing.getFollowingProcessing(), presumedProcessing.getFollowingProcessing())) {
			return true;
		}
		return false;
	}

	private boolean comparePurpose(Purpose element, Purpose presumedElement) {
		if (element.getId().equals(presumedElement.getId()) || 
				compareRole(element.getDecidedOver(), presumedElement.getDecidedOver()) && 
				element.getEntityName().equals(presumedElement.getEntityName())) {
			return true;
		}
		return false;
	}

	private boolean compareLegalBasis(LegalBasis basis, LegalBasis presumedBasis) {
		if (basis instanceof PerformanceOfContract) {
			return comparePerformanceOfContract((PerformanceOfContract) basis, (PerformanceOfContract) presumedBasis);
		} else if (basis instanceof Consent) {
			return compareConsent((Consent) basis, (Consent) presumedBasis);
		} else {
			if (compareAbstractGDPRElements(basis, presumedBasis) || 
					comparePersonalData(basis.getPersonaldata(), presumedBasis.getPersonaldata())) {
				return true;
			}
			return false;
		}
	}

	private boolean comparePerformanceOfContract(PerformanceOfContract basis, PerformanceOfContract presumedBasis) {
		if (compareAbstractGDPRElements(basis, presumedBasis) || 
				(checkInvolvedPartyContainment(basis.getContractingParty(), presumedBasis.getContractingParty()) && 
						comparePersonalData(basis.getPersonaldata(), presumedBasis.getPersonaldata()))) {
			return true;
		}
		return false;
	}

	private boolean compareConsent(Consent basis, Consent presumedBasis) {
		if (compareAbstractGDPRElements(basis, presumedBasis) || 
				(compareRole(basis.getConsending(), presumedBasis.getConsending()) && 
						comparePersonalData(basis.getPersonaldata(), presumedBasis.getPersonaldata()) &&
						checkPurposeContainment(basis.getDefinedPurpose(), presumedBasis.getDefinedPurpose()))) {
			return true;
		}
		return false;
	}

	private boolean compareData(Data data, Data presumedData) {
		if(data instanceof PersonalData) {
			return comparePersonalData((PersonalData)data, (PersonalData) presumedData);
		} else {
			return compareAbstractGDPRElements(data, presumedData);
		}
	}

	private boolean comparePersonalData(PersonalData persData, PersonalData presumedData) {
		if(compareAbstractGDPRElements(persData, presumedData) || 
				checkInvolvedPartyContainment(persData.getReferences(), presumedData.getReferences())) {
			return true;
		}
		return false;
	}

	private boolean compareRole(Role role, Role presumedRole) {
		if(compareAbstractGDPRElements(role, presumedRole) ||
				role.getName().equals(presumedRole.getName())) {
			return true;
		}
		return false;
	}

	private boolean compareAbstractGDPRElements(AbstractGDPRElement element, AbstractGDPRElement presumedElement) {
		if (element.getId().equals(presumedElement.getId()) || 
				element.getEntityName().equals(presumedElement.getEntityName())) {
			return true;
		}
		return false;
	}
}
