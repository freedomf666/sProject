module SecurityModule{
    requires java.desktop;
    requires java.prefs;
    requires com.google.common;
    requires com.google.gson;
    requires ImageModule;
    requires miglayout;
    opens com.udacity.catpoint.securityService.data to com.google.gson;
}
