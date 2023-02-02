module ImageModule {
    exports com.udacity.catpoint.imageService to SecurityModule, com.udacity.catpoint.securityService.application;
    requires java.desktop;
    requires software.amazon.awssdk.regions;
    requires org.slf4j;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.services.rekognition;
}
