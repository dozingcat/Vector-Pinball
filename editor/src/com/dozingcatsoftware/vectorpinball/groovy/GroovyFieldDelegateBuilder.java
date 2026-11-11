package com.dozingcatsoftware.vectorpinball.groovy;

import java.io.IOException;
import java.util.Arrays;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;

import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

public class GroovyFieldDelegateBuilder {

    public static GroovyFieldDelegate createFromScript(String script, ClassLoader classLoader) {
        CompilerConfiguration config = new CompilerConfiguration();

        SecureASTCustomizer secureAst = new SecureASTCustomizer();
        secureAst.setIndirectImportCheckEnabled(true);
        // TODO: Determine which classes should be accessible.
        secureAst.setImportsBlacklist(Arrays.asList("java.lang.Process"));
        secureAst.setReceiversClassesBlackList(Arrays.asList(String.class));

        ImportCustomizer imports = new ImportCustomizer();
        imports.addImport("Color", "com.dozingcatsoftware.vectorpinball.model.Color");
        imports.addImport("Shape", "com.dozingcatsoftware.vectorpinball.model.Shape");
        imports.addImport("Vector2", "com.badlogic.gdx.math.Vector2");

        config.addCompilationCustomizers(secureAst, imports);

        try (GroovyClassLoader gcl = new GroovyClassLoader(classLoader, config)) {
            @SuppressWarnings("unchecked")
            Class<? extends Script> groovyScriptClass = gcl.parseClass(script);
            return (new GroovyFieldDelegate()).initWithScript(groovyScriptClass.newInstance());
        }
        catch (IOException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

}
