package mdpa.lnr.analysis.tests;

import java.nio.file.Paths;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;
import tools.mdsd.library.standalone.initialization.StandaloneInitializerBuilder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mdpa.lnr.analysis.AnalysisEngine;

class BasicAnalysisTest {
	private static final String PLUGIN_PATH = "mdpa.lnr.analysis.tests";
	private static final String GDPR_MODEL_PATH = "LNRTest/My.gdpr";
	private static final String CONTEXT_PROPERTIES_PATH = "LNRTest/My.contextproperties";
	private static final String LNR_MODEL_PATH = "LNRTest/My.legalnormrules";

	@BeforeAll
	static void setUpBeforeAll() throws Exception {
		initStandalone();
	}

	@BeforeEach
	void setUpBeforeEach() throws Exception {
	}

	@Test
	void test() {
		AnalysisEngine analysis = new AnalysisEngine(createPluginURI(GDPR_MODEL_PATH), createPluginURI(CONTEXT_PROPERTIES_PATH), createPluginURI(LNR_MODEL_PATH));
		System.out.println("");
		analysis.analyze();
	}
	
    private URI createPluginURI(String relativePath) {
        String path = Paths.get(PLUGIN_PATH, "models", relativePath)
            .toString();
        return URI.createPlatformPluginURI(path, false);
    }
    
    private static void initStandalone() {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("gdpr", new XMIResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("contextproperties", new XMIResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("legalnormrules", new XMIResourceFactoryImpl());
		
        try {
            StandaloneInitializerBuilder.builder()
                .registerProjectURI(BasicAnalysisTest.class, PLUGIN_PATH)
                .build()
                .init();

        } catch (StandaloneInitializationException e) {
            e.printStackTrace();
        }
    }
}
