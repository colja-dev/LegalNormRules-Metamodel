package mdpa.lnr.metamodel.api;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;

import mdpa.lnr.metamodel.LegalNormRules.LegalNormRules;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;
import tools.mdsd.library.standalone.initialization.StandaloneInitializerBuilder;

public class LegalNormRulesApi {
	private static final String PLUGIN_PATH = "mdpa.lnr.metamodel.api";

	private ResourceSet resources = new ResourceSetImpl();
	
	private LegalNormRules lnr;
	
	public LegalNormRulesApi(URI lnrModelURI) {
		if(EMFPlugin.IS_ECLIPSE_RUNNING) {
			initStandalone();
		}
		
		System.out.println("Loading legal norm rules model instance at " + lnrModelURI.path());
		loadLNRModel(lnrModelURI);
		
		//this could result in an error when loading the other models after
		System.out.println("Resolving resources.");
		resolveResources();
	}
	
	public LegalNormRules getLegalNormRules() {
		return this.lnr;
	}
	
	private void loadLNRModel(URI lnrModelURI) {
		this.lnr = (LegalNormRules) this.loadResource(lnrModelURI);		
	}
	
	private void resolveResources() {
		List<Resource> loadedResources = null;
		do {
			loadedResources = new ArrayList<>(this.resources.getResources());
			loadedResources.forEach(it->EcoreUtil.resolveAll(it));
		} while (loadedResources.size() != this.resources.getResources().size());
	}
	
	private EObject loadResource(URI modelURI) {
		Resource resource = this.resources.getResource(modelURI, true);
		if (resource == null) {
			throw new IllegalArgumentException(String.format("Model with URI %s could not be loaded", modelURI));
		} else if (resource.getContents().isEmpty()) {
			throw new IllegalArgumentException(String.format("Model with URI %s is empty", modelURI));
		}
		return resource.getContents().get(0);
	}
	
    private boolean initStandalone() {
        try {
            StandaloneInitializerBuilder.builder()
                .registerProjectURI(LegalNormRulesApi.class, LegalNormRulesApi.PLUGIN_PATH)
                .build()
                .init();
            return true;

        } catch (StandaloneInitializationException e) {
            e.printStackTrace();
            return false;
        }
    }
	
    private URI createRelativePluginURIFromProjectPath(String projectName, String relativePath) {
        String path = Paths.get(projectName, relativePath)
            .toString();
        return URI.createPlatformPluginURI(path, false);
    }
    
    private URI createRelativePluginURIFromAbsolutePath(String absolutePath) {
    	return URI.createPlatformPluginURI(absolutePath, false);
    }

}
