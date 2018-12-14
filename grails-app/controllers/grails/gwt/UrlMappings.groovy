package grails.gwt

class UrlMappings {

    static mappings = {
        "/gwt/$module/rpc"(controller: "gwt", action: "index")
    }
}
