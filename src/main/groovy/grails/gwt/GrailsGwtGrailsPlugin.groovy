package grails.gwt

import grails.plugins.*
import org.codehaus.groovy.grails.plugins.gwt.ActionHandlerArtefactHandler
import org.codehaus.groovy.grails.plugins.gwt.DefaultGwtServiceInterfaceGenerator
import org.codehaus.groovy.grails.plugins.gwt.GwtCacheControlFilter

class GrailsGwtGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.0.1 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Grails Gwt" // Headline display name of the plugin
    def author = "Denis Halupa"
    def authorEmail = "denis.halupa@gmail.com"
    def description = '''\
Incorporates GWT into Grails. In particular, GWT host pages can be
GSPs and standard Grails services can be used to handle client RPC
requests.
'''
    def profiles = ['web']

    def observe = ["services"]
    def watchedResources = "file:./grails-app/actionHandlers/**/*ActionHandler.groovy"
    def artefacts = [ActionHandlerArtefactHandler]

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/grails-gwt"

    def srcDir = "src/main/java"

    Closure doWithSpring() {
        { ->
            // Create Spring beans for all the actions defined by the user.
            final c = configureActionHandler.clone()
            c.delegate = delegate

            application.actionHandlerClasses.each { handlerClass ->
                log.info "Registering action handler: ${handlerClass.fullName}"
                c.call(handlerClass)
            }

            // Bean for generating RPC interfaces for services.
            gwtInterfaceGenerator(DefaultGwtServiceInterfaceGenerator)

            if (application.config.gwt.nocache.filter.enabled) {
                gwtCacheControlFilter(GwtCacheControlFilter)
            }
        }
    }

    void doWithDynamicMethods() {
        def interfaceGenerator = ctx.getBean("gwtInterfaceGenerator")

        grailsApplication.serviceClasses.each { serviceWrapper ->
            if (interfaceGenerator.isGwtExposed(serviceWrapper.clazz)) {
                //TODO Have to be checked!!!!
                //WebMetaUtils.registerCommonWebProperties(serviceWrapper.clazz.metaClass, application)
            }
        }
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
        def application = event.application
        if (application.isServiceClass(event.source)) {
            def interfaceGenerator = event.ctx.getBean("gwtInterfaceGenerator")
            def serviceWrapper = application.getServiceClass(event.source?.name)

            if (interfaceGenerator.isGwtExposed(serviceWrapper.clazz)) {
                //TODO Have to be checked!!!!
        //        WebMetaUtils.registerCommonWebProperties(serviceWrapper.clazz.metaClass, application)
            }
        } else if (application.isActionHandlerClass(event.source)) {
            // Update the artifact. Without this step, the reloading
            // won't work.
            def grailsClass = application.addArtefact(ActionHandlerArtefactHandler.TYPE, event.source)

            // Re-register the action handler bean.
            def beans = beans {
                final c = configureActionHandler.clone()
                c.delegate = delegate
                c.call(grailsClass)
            }

            if (event.ctx) {
                beans.registerBeans(event.ctx)
            }
        }
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }

    /**
     * Adds the appropriate Spring bean for the given action handler
     * descriptor. Note that no check is performed on whether the
     * descriptor represents an action handler or not. The bean is
     * registered under the name "gwt<actionHandleClass>".
     */
    def configureActionHandler = { grailsClass ->
        "gwt${grailsClass.shortName}"(grailsClass.clazz) { bean ->
            bean.autowire = "byName"
        }
    }

    /**
     * Searches a given directory for any GWT module files, and
     * returns a list of their fully-qualified names.
     * @param searchDir A string path specifying the directory
     * to search in.
     * @return a list of fully-qualified module names.
     */
    def findModules(searchDir) {
        def modules = []
        def baseLength = searchDir.size()

        searchDir = new File(searchDir)
        if (!searchDir.exists()) return modules

        searchDir.eachFileRecurse { file ->
            // Replace Windows separators with Unix ones.
            file = file.path.replace('\\' as char, '/' as char)

            // Chop off the search directory.
            file = file.substring(baseLength + 1)

            // Now check whether this path matches a module file.
            def m = file =~ /([\w\/]+)\.gwt\.xml$/
            if (m.count > 0) {
                // Extract the fully-qualified module name.
                modules << m[0][1].replace('/' as char, '.' as char)
            }
        }

        return modules
    }
}
