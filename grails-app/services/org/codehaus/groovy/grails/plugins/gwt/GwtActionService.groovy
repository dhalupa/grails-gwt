package org.codehaus.groovy.grails.plugins.gwt

import grails.plugins.gwt.shared.Action
import grails.plugins.gwt.shared.Response
import grails.transaction.Transactional
import grails.util.GrailsNameUtils
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Implementation of the plugin's action service, which handles all
 * GWT client requests that use actions. The service simply delegates
 * the action processing to the appropriate action handler. The action
 * handler should have the same name as the corresponding action, just
 * with a "Handler" suffix.
 */
@Transactional
class GwtActionService implements grails.plugins.gwt.client.GwtActionService, ApplicationContextAware {
    static expose = ["gwt"]

    ApplicationContext applicationContext

    Response execute(Action action) {
        // Get the class name of the action, because we need it to find
        // the corresponding action bean.
        def name = GrailsNameUtils.getShortName(action.getClass())

        // Prefix the name with "gwt" and add a "Handler" suffix to get
        // hold of the appropriate bean.
        def handlerBeanName = "gwt${name}Handler"
        if (applicationContext.containsBean(handlerBeanName)) {
            def actionBean = applicationContext.getBean(handlerBeanName)
            return actionBean.execute(action)
        } else {
            throw new RuntimeException("No action handler configured for ${name}")
        }
    }
}